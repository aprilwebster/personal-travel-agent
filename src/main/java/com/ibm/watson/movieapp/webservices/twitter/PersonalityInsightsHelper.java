package com.ibm.watson.movieapp.webservices.twitter;

import java.net.URI;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
//import org.glassfish.hk2.utilities.AbstractActiveDescriptor;

public class PersonalityInsightsHelper {
	public static final String PI_URL_PROP_NAME = "pi_url";
	public static final String PI_USERNAME_PROP_NAME = "pi_username";
	public static final String PI_PASSWORD_PROP_NAME = "pi_password";

	public static final String PI_URL_DEFAULT = "https://gateway.watsonplatform.net/personality-insights/api";
	public static final String PI_PROFILE_API_PATH = "v2/profile";

	URI uri;
	Client client;

	public PersonalityInsightsHelper(Properties props) {
		if (StringUtils.isEmpty(props.getProperty(PI_USERNAME_PROP_NAME))
			|| StringUtils.isEmpty(props.getProperty(PI_PASSWORD_PROP_NAME))
			|| StringUtils.isEmpty(props.getProperty(PI_URL_PROP_NAME))) {
			System.err
					.println("Some PI properties not found, check your twittersample.properties file");
		}
		uri = UriBuilder
				.fromUri(props.getProperty(PI_URL_PROP_NAME, PI_URL_DEFAULT))
				.path(PI_PROFILE_API_PATH).build();
		ClientConfig clientConfig = new DefaultClientConfig();
		client = Client.create(clientConfig);
		HTTPBasicAuthFilter basicAuthFilter = new HTTPBasicAuthFilter(
				props.getProperty(PI_USERNAME_PROP_NAME),
				props.getProperty(PI_PASSWORD_PROP_NAME));
		client.addFilter(basicAuthFilter);
	}

	public String getProfileJSON(String contentItemsJson, boolean includeRaw) {
		System.out.println("DEBUG: the contentItemsJson is " + contentItemsJson);
		String test = client.resource(uri)
				.queryParam("include_raw", Boolean.toString(includeRaw))
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.entity(contentItemsJson, MediaType.APPLICATION_JSON_TYPE)
				.post(String.class);
		
		System.out.println("DEBUG PersonalityInsightsHelper: " + test.contains("\n"));
		String newline = System.getProperty("line.separator");
		boolean hasNewline = test.contains(newline);
		System.out.println("DEBUG PersonalityInsightsHelper: " + hasNewline);
		
		return test;
	}

	public String getProfileCSV(String contentItemsJson, boolean includeHeaders) {
		return client.resource(uri)
				.queryParam("headers", Boolean.toString(includeHeaders))
				.accept("text/csv")
				.entity(contentItemsJson, MediaType.APPLICATION_JSON_TYPE)
				.post(String.class);
	}
	
	/*public JSONObject getProfileJSONObject(String contentItemsJson, boolean includeRaw) {
		System.out.println("DEBUG: the contentItemsJson is " + contentItemsJson);
		return client.resource(uri)
				.queryParam("include_raw", Boolean.toString(includeRaw))
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.entity(contentItemsJson, MediaType.APPLICATION_JSON_TYPE)
				.post(JSONObject.class);
	}*/


}
