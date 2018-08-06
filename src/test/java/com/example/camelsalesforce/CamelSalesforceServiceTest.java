package com.example.camelsalesforce;

import java.io.InputStream;
import java.util.List;


import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.salesforce.api.dto.bulk.BatchInfo;
import org.apache.camel.component.salesforce.api.dto.bulk.JobInfo;
import org.apache.camel.component.salesforce.api.dto.bulk.JobStateEnum;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JaxbDataFormat;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.BootstrapWith;

import com.example.camelsalesforce.services.Greetings;

@RunWith(CamelSpringBootRunner.class)
@BootstrapWith(SpringBootTestContextBootstrapper.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@MockEndpoints
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class CamelSalesforceServiceTest {

	private static Logger LOG = Logger.getLogger(CamelSalesforceServiceTest.class);

	private static final String RESOURCE_EXAMPLE_FILE = "/sample_data/FL_insurance_sample.csv";

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Produce(uri = "direct:processFile")
	private ProducerTemplate processFileProducer;

	@EndpointInject(uri = "mock:processFileMockOut")
	private MockEndpoint processFileMockOut;
	
	@Autowired
	ModelCamelContext camelContext;

	@Before
	public void configure() throws Exception {
		camelContext.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				rest("/services/oauth2/token")
				.description("Mock Salesforce Login Endpoint")
				.post().consumes("application/json")
				.outType(String.class)
				.route().routeId("mock_login_route")
				.process(exchange -> {
					exchange.getIn().setBody("{ \"access_token\": \"token\", \"instance_url\": \"http://localhost:8080/camel/\" }");
				});
				
				JaxbDataFormat jaxbDataFormat = new JaxbDataFormat();
				jaxbDataFormat.setContextPath(JobInfo.class.getPackage().getName());
				
				rest("/services/async/37.0/job/{jobid}/batch")
				.description("Mock createBatch")
				.post().consumes("application/json").produces("application/xml")
				.outType(String.class)
				.route().routeId("mock_create_batch_route")
				.to("mock:processFileMockOut")
				.log("Batch Request body: ${body}")
				.log("Batch job id: ${header.jobid}")
				.process(exchange -> {
					BatchInfo batchInfo = new BatchInfo();
					batchInfo.setId(RandomStringUtils.randomAlphanumeric(6));
					exchange.getIn().setBody(batchInfo);
				})
				.log("Batch request processed... now marshalling...")
				.marshal(jaxbDataFormat)
				.log("Batch Request Response: ${body}");
				
				rest("/services/async/37.0/job")
				.description("Mock createJob")
				.post().consumes("application/xml").produces("application/xml")
				.outType(String.class)
				.route().routeId("mock_create_job_route")
				.unmarshal(jaxbDataFormat)
				.process(exchange -> {
					JobInfo jobInfo = exchange.getIn().getBody(JobInfo.class);
					jobInfo.setState(JobStateEnum.OPEN);
					jobInfo.setId(RandomStringUtils.randomAlphanumeric(6));
				})
				.marshal(jaxbDataFormat)
				.log("Response body: ${body}");
			}
		});
	}

	@Test
	public void testProcessData() throws Exception {
		try (InputStream inputStream = getClass().getResourceAsStream(RESOURCE_EXAMPLE_FILE)) {
			processFileProducer.sendBodyAndHeader(inputStream, "CamelFileName",
					RESOURCE_EXAMPLE_FILE.substring(RESOURCE_EXAMPLE_FILE.lastIndexOf("/")));
			processFileMockOut.setMinimumExpectedMessageCount(700);
			processFileMockOut.assertIsSatisfied();

			List<Exchange> exchanges = processFileMockOut.getExchanges();
			byte[] exchange1Body = exchanges.get(0).getIn().getBody(byte[].class);
			Assert.assertNotNull(exchange1Body);
			byte[] exchange2Body = exchanges.get(1).getIn().getBody(byte[].class);
			Assert.assertNotNull(exchange2Body);
		}
	}

	@Test
	public void greetingsShouldReturnFallbackMessage() throws Exception {
		Assert.assertEquals("Hello, jacopo", this.restTemplate
				.getForObject("http://localhost:" + port + "/camel/greetings/jacopo", Greetings.class).getGreetings());
	}

	@Test
	public void healthShouldReturnOkMessage() throws Exception {
		Assert.assertEquals("{\"status\":\"UP\"}",
				this.restTemplate.getForObject("http://localhost:" + port + "/health", String.class));
	}
}