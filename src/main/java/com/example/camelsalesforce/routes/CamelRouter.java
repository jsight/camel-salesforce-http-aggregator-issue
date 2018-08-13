package com.example.camelsalesforce.routes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.salesforce.SalesforceComponent;
import org.apache.camel.component.salesforce.api.dto.bulk.ConcurrencyModeEnum;
import org.apache.camel.component.salesforce.api.dto.bulk.ContentType;
import org.apache.camel.component.salesforce.api.dto.bulk.JobInfo;
import org.apache.camel.component.salesforce.api.dto.bulk.OperationEnum;
import org.apache.camel.model.dataformat.CsvDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.processor.aggregate.AggregationStrategy;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.camelsalesforce.processors.FieldMapping;
import com.example.camelsalesforce.processors.MultipartProcessor;
import com.example.camelsalesforce.processors.FieldMapping.FieldMappingEntry;
import com.example.camelsalesforce.services.Greetings;

/**
 * A simple Camel REST DSL route that implement the greetings service.
 * 
 */
@Component
public class CamelRouter extends RouteBuilder {

	private static final String CSV_HEADER = "csvHeader";
	private static Logger LOG = Logger.getLogger(CamelRouter.class);
	
	@Autowired
	private SalesforceComponent salesforceComponent;
	
	@Override
    public void configure() throws Exception {
		
		getContext().addComponent("salesforce", salesforceComponent);
		
        // @formatter:off
        restConfiguration()
                .apiContextPath("/api-doc")
                .apiProperty("api.title", "Greeting REST API")
                .apiProperty("api.version", "1.0")
                .apiProperty("cors", "true")
                .apiProperty("base.path", "/camel")
                .apiProperty("api.path", "/")
                .apiProperty("host", "")
//                .apiProperty("schemes", "")
                .apiContextRouteId("doc-api")
                .endpointProperty("chunked", "true")
                .component("servlet");
        
        rest("/greetings/").description("Greeting to {name}")
            .get("/{name}").outType(Greetings.class)
                .route().routeId("greeting-api")
                .to("direct:greetingsImpl")
                .marshal().json(JsonLibrary.Jackson);
        
        CsvDataFormat csv = new CsvDataFormat();
        //csv.setLazyLoad(true);
        
        JacksonDataFormat jacksonDataFormat = new JacksonDataFormat();
        
        rest("/fileupload/").description("Handles file uploads")
        	.post()
        	.consumes("multipart/form-data")
        	.outType(String.class)
        	.route().routeId("file-upload")
        	.bean(MultipartProcessor.class)
        	.to("direct:processFile");
        	
    	from("direct:processFile")
    	.streamCaching()
    	.unmarshal(csv)
    	.process(exchange -> {
        		@SuppressWarnings("unchecked")
        		List<List<String>> csvIterator = (List<List<String>>)exchange.getIn().getBody();
        		List<String> headerLine = csvIterator.remove(0);
        		exchange.getIn().setHeader(CSV_HEADER, headerLine);
    	})
    	.process(exchange -> {
    		String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
    		if (fileName.contains("FL_insurance")) {
    			exchange.getIn().setHeader("fieldMapping", FieldMapping.TRANSLATE);
    			exchange.getIn().setHeader("objectType", "ExampleObject");
    		} else {
    			throw new IllegalArgumentException("Unrecognized file type: " + fileName);
    		}
    	})
    	.to("seda:enrichWithJobId?waitForTaskToComplete=Always")
//    	.enrich("direct:createJobAndGetID", new AggregationStrategy() {
//			@Override
//			public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
//				oldExchange.getIn().setHeader("jobId", newExchange.getIn().getBody(JobInfo.class).getId());
//				return oldExchange;
//			}
//		})
		.process(exchange -> {
	    	List<Map<String, Object>> results = new ArrayList<>();
	    	for (List<String> fieldData : (List<List<String>>)exchange.getIn().getBody(List.class)) {
				@SuppressWarnings("unchecked")
				List<String> headerFields = (List<String>)exchange.getIn().getHeader(CSV_HEADER);
				
				FieldMapping fieldMapping = exchange.getIn().getHeader("fieldMapping", FieldMapping.class);
				Map<String, Object> row = new HashMap<>();
				for (int i = 0; i < fieldData.size(); i++) {
					String name = headerFields.get(i);
					String data = fieldData.get(i);
					if (StringUtils.isBlank(data))
						continue;
					
					FieldMappingEntry mappingEntry = fieldMapping.get(name);
					if (mappingEntry == null) {
						// Skip it if there is no mapping for this field
						continue;
					}
					String dstName = mappingEntry.getDstName();
					Object dataConverted = exchange.getContext().getTypeConverter().convertTo(mappingEntry.getDataType(), data);
					row.put(dstName, dataConverted);
				}
				results.add(row);
	    	}
	    	exchange.getIn().setBody(results);
		})
    	.split(body()).shareUnitOfWork()
    	.aggregate(constant(true), new ArrayListAggregationStrategy())
    		.completionSize(50)
    		.completionInterval(1000)
    		.completeAllOnStop()
    	.process(exchange -> {
    		LOG.info("Sending batch: " + exchange.getIn().getBody(ArrayList.class).size());
    	})
    	.marshal(jacksonDataFormat)
    	.toD("salesforce:createBatch?contentType=json");
        
    	// FIXME -- This is just there as a separate seda pipe to get around an issue with camel-salesforce.
    	//  JIRA to be filed soon.
    	from("seda:enrichWithJobId").enrich("direct:createJobAndGetID", new AggregationStrategy() {
			@Override
			public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
				oldExchange.getIn().setHeader("jobId", newExchange.getIn().getBody(JobInfo.class).getId());
				return oldExchange;
			}
		});
    	
    	from("direct:createJobAndGetID")
    	.process(exchange -> {
    		JobInfo jobInfo = new JobInfo();
    		jobInfo.setOperation(OperationEnum.INSERT);
    		jobInfo.setObject(exchange.getIn().getHeader("objectType", String.class));
    		jobInfo.setContentType(ContentType.CSV);
    		jobInfo.setConcurrencyMode(ConcurrencyModeEnum.SERIAL);
    		exchange.getIn().setBody(jobInfo);
    	})
    	.toD("salesforce:createJob")
    	;
    	
        from("direct:greetingsImpl").description("Greetings REST service implementation route")
            .to("bean:greetingsService?method=getGreetings");     
        // @formatter:on
    }

	public class ArrayListAggregationStrategy implements AggregationStrategy {

		public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
			Object newBody = newExchange.getIn().getBody();
			ArrayList<Object> list = null;
			if (oldExchange == null) {
				LOG.info("New one");
				list = new ArrayList<Object>();
				list.add(newBody);
				newExchange.getIn().setBody(list);
				return newExchange;
			} else {
				list = oldExchange.getIn().getBody(ArrayList.class);
				list.add(newBody);
				return oldExchange;
			}
		}
	}
}