package com.example.camelsalesforce.processors;

import javax.servlet.MultipartConfigElement;

import org.springframework.boot.context.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import io.undertow.servlet.api.DeploymentInfo;

@Component
public class UndertowMultipartCustomizer {
	
	@Bean
	public UndertowEmbeddedServletContainerFactory getUndertowEmbeddedServletContainerFactory() {
		UndertowEmbeddedServletContainerFactory factory = new UndertowEmbeddedServletContainerFactory();
		factory.addDeploymentInfoCustomizers(undertowDeploymentInfoCustomizer());
		return factory;
	}
	
    public UndertowDeploymentInfoCustomizer undertowDeploymentInfoCustomizer() {
        return new UndertowDeploymentInfoCustomizer() {
            @Override
            public void customize(DeploymentInfo deploymentInfo) {
            	MultipartConfigFactory multipartConfigFactory = new MultipartConfigFactory();
        		MultipartConfigElement multipartConfigElement = multipartConfigFactory.createMultipartConfig();
        		deploymentInfo.setDefaultMultipartConfig(multipartConfigElement);
            }
        };
    }
}
