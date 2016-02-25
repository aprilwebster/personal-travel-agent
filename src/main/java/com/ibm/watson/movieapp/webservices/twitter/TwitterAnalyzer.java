package com.ibm.watson.movieapp.webservices.twitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import twitter4j.Status;


public class TwitterAnalyzer {
	private static final String TWITTER_PROPERTIES_FILE = "/locale/twitter.properties";
	private static final Integer TWEET_COUNT = 200;
	private static final Boolean DEBUG = true;
	
	public String getPersonalityProfile (String handle) throws Exception {
		
		File file = new File(TWITTER_PROPERTIES_FILE);
		
		if(DEBUG){
			System.out.println("DEBUG TwitterAnalyzer: properties file is " + file.getName());
			System.out.println("DEBUG TwitterAnalyzer: properties file path is " + file.getPath());
		}
		
		Properties props = new Properties();
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(TWITTER_PROPERTIES_FILE);
		if (inputStream != null) {
			props.load(inputStream);
		} else {
			throw new FileNotFoundException("Twitter Analyzer: twitter property file '" + TWITTER_PROPERTIES_FILE + "' not found in the classpath");
		}

		if(DEBUG){
			System.out.println("DEBUG TwitterAnalyzer: properties are " + props);
		}
		
		// Instantiate Twitter4JHelper to retrieve tweets 
		// Instantiate PersonalityInsightsHelper to get a personality profile, given tweets
		Twitter4JHelper twitterHelper = new Twitter4JHelper(props);
		PersonalityInsightsHelper piHelper = new PersonalityInsightsHelper(props);	
		
		// Language(s) for the tweets to be retrieved from Twitter (e.g., "en" for English, "es" for Spanish)
		HashSet<String> langs = new HashSet<String>();
		langs.add("en");

		// Retrieve the tweets and convert them to the format required by the Personality Insights Service
		List<Status> tweets = twitterHelper.getTweets(handle, langs, TWEET_COUNT);
		String contentItemsJson = twitterHelper.convertTweetsToPIContentItems(tweets);
		
		// Retrieve the personality Profile
		String personalityProfile = piHelper.getProfileJSON(contentItemsJson, false);
		
		
		if(DEBUG){
			System.out.println("DEBUG TwitterAnalyzer profile from PI" + personalityProfile);
			JsonParser parser = new JsonParser();
			JsonObject obj = (JsonObject) parser.parse(personalityProfile);
			System.out.println("DEBUG TwitterAnalyzer: " + obj.toString());
		}

		return personalityProfile;
	}
}
