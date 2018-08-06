package com.example.camelsalesforce.processors;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.http.common.HttpMessage;

public class MultipartProcessor implements Processor {

	@Override
	public void process(Exchange exchange) throws Exception {
		HttpMessage message = exchange.getIn(HttpMessage.class);
		HttpServletRequest request = message.getRequest();
		for (Part part : request.getParts()) {
			String filename = part.getSubmittedFileName();
			//String endpointPath = exchange.getFromEndpoint().getEndpointUri();
			InputStream partInputStream = exchange.getContext().getTypeConverter().convertTo(InputStream.class, part.getInputStream());
			exchange.getIn().setHeader("CamelFileName", filename);
			exchange.getIn().setBody(partInputStream);
			break;
		}
	}
}
