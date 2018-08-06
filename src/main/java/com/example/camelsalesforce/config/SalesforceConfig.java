package com.example.camelsalesforce.config;

import org.apache.camel.component.salesforce.SalesforceComponent;
import org.apache.camel.component.salesforce.SalesforceEndpointConfig;
import org.apache.camel.component.salesforce.SalesforceLoginConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="example.salesforce")
public class SalesforceConfig {

	private String username;
	private String password;
	private String url;
	private String version;
	private String clientId;
	private String clientSecret;
	
	@Bean
	public SalesforceLoginConfig salesforceLoginConfig() {
		SalesforceLoginConfig salesforceLoginConfig = new SalesforceLoginConfig();
		salesforceLoginConfig.setUserName(this.username);
		salesforceLoginConfig.setPassword(this.password);
		salesforceLoginConfig.setLoginUrl(this.url);
		salesforceLoginConfig.setClientId(this.clientId);
		salesforceLoginConfig.setClientSecret(this.clientSecret);
		salesforceLoginConfig.setLazyLogin(true);
		return salesforceLoginConfig;
	}
	
	@Bean
	public SalesforceEndpointConfig salesforceEndpointConfig() {
		SalesforceEndpointConfig salesforceEndpointConfig = new SalesforceEndpointConfig();
		salesforceEndpointConfig.setApiVersion(version);
		return salesforceEndpointConfig;
	}
	
	@Bean
	public SalesforceComponent salesforceComponent() {
		SalesforceComponent salesforceComponent = new SalesforceComponent();
		salesforceComponent.setConfig(salesforceEndpointConfig());
		salesforceComponent.setLoginConfig(salesforceLoginConfig());
		return salesforceComponent;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	
	
}
