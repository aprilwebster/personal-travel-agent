/* Copyright IBM Corp. 2015
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.watson.travelagentapp.dialog.rest;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;



import com.ibm.watson.travelagentapp.webservices.googlemaps.GoogleMapsProxyResource;
import com.ibm.watson.travelagentapp.utilities.Similarity;
import com.ibm.watson.travelagentapp.webservices.twitter.TwitterAnalyzer;
import com.ibm.watson.travelagentapp.webservices.watson.WatsonEmotionServiceProxyResource;


import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.google.gson.JsonArray;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import com.ibm.watson.developer_cloud.dialog.v1.model.Conversation;
import com.ibm.watson.developer_cloud.dialog.v1.model.*;
import com.ibm.watson.developer_cloud.dialog.v1.model.NameValue;
import com.ibm.watson.travelagentapp.dialog.exception.WatsonTheatersException;
import com.ibm.watson.travelagentapp.dialog.payload.MoviePayload;

import com.ibm.watson.travelagentapp.dialog.payload.ServerErrorPayload;
import com.ibm.watson.travelagentapp.dialog.payload.StorePayload;

import com.ibm.watson.travelagentapp.dialog.payload.WDSConversationPayload;
import com.ibm.watson.travelagentapp.dialog.rest.UtilityFunctions;

import com.ibm.watson.developer_cloud.dialog.v1.DialogService;


// Importing PI eclipse project to get a personality profile given a twitter handle
//import com.ibm.watson.personalityinsights.sample.twitter.TwitterAnalyzer;


/**
 * <p>
 * Proxy class to communicate with Watson Dialog Service
 * (WDS) to generate chat responses to the user input.
 * </p>
 * <p>
 * There are multiple JAX-RS entry points to this class, depending on the task to be performed. eg.: /postConversation to post the user input to the WDS
 * service and get a response.
 * </p>
 * <p>
 * In addition, there are various helper methods to parse response text, etc.
 * </p>
 */

@Path("/bluemix")
public class WDSBlueMixProxyResource {
    private static String wds_base_url;
    private static DialogService dialogService = null;
    private static String dialog_id;
    private static String classifier_id;
    private static String username_dialog = null;
    private static String password_dialog = null;
    private static String walking_distance = "walking distance";
    
    private static String personalized_prompt_movie_selected = "USER CLICKS BOX"; //$NON-NLS-1$
    private static String personalized_prompt_store_selected = "USER CLICKS STORE"; //$NON-NLS-1$
    private static String personalized_prompt_movies_returned = "UPDATE NUM_MOVIES"; //$NON-NLS-1$
    
    private static final String WATSON_BASE_URL = "gateway.watsonplatform.net/";
	private static final String WATSON_DIALOG_URL = "dialog/api/v1/dialogs/";
    private static final String GET_PROFILE_VARIABLES_URL = "/profile";
    
    private static final String GOOGLE_MAPS_EMBED_BASE_URL = "https://www.google.com/maps/embed/v1/";
    private static final String GOOGLE_MAPS_EMBED_DIRECTIONS_ENDPOINT = "directions";
    private static final String GOOGLE_MAPS_EMBED_SEARCH_ENDPOINT = "search";
    private static final String GOOGLE_MAPS_EMBED_API_KEY = "AIzaSyB1RD2gilBuJjZPQP500vCZPMoDqGfBav8";
    
    private static Boolean DEBUG = true;
    
    
    
    static {
        loadStaticBluemixProperties();
        useDialogServiceInstance();
    }

    /**
     * Loads VCAP_SERVICES environment variables required to make calls to Dialog and Classifier services.
     */
    private static void loadStaticBluemixProperties() {
        String envServices = System.getenv("VCAP_SERVICES"); //$NON-NLS-1$
        if (envServices != null) {
            UtilityFunctions.logger.info(Messages.getString("WDSBlueMixProxyResource.VCAP_SERVICES_ENV_VAR_FOUND")); //$NON-NLS-1$
            JsonObject services = new JsonParser().parse(envServices).getAsJsonObject();
            UtilityFunctions.logger.info(Messages.getString("WDSBlueMixProxyResource.VCAP_SERVICES_JSONOBJECT_SUCCESS")); //$NON-NLS-1$

            // Get credentials for Dialog Service
            JsonArray arr = (JsonArray) services.get("dialog"); //$NON-NLS-1$
            if (arr != null && arr.size() > 0) {
                services = arr.get(0).getAsJsonObject();
                JsonObject credentials = services.get("credentials").getAsJsonObject(); //$NON-NLS-1$
                wds_base_url = credentials.get("url").getAsString(); //$NON-NLS-1$
                if (credentials.get("username") != null && !credentials.get("username").isJsonNull()) { //$NON-NLS-1$ //$NON-NLS-2$
                    username_dialog = credentials.get("username").getAsString(); //$NON-NLS-1$
                    UtilityFunctions.logger.info(Messages.getString("WDSBlueMixProxyResource.FOUND_WDS_USERNAME")); //$NON-NLS-1$
                }
                if (credentials.get("password") != null && !credentials.get("password").isJsonNull()) { //$NON-NLS-1$ //$NON-NLS-2$
                    password_dialog = credentials.get("password").getAsString(); //$NON-NLS-1$
                    UtilityFunctions.logger.info(Messages.getString("WDSBlueMixProxyResource.FOUND_WDS_PASSWORD")); //$NON-NLS-1$
                }
            }

 
        } else {
            UtilityFunctions.logger.error(Messages.getString("WDSBlueMixProxyResource.VCAP_SERVICES_CANNOT_LOAD")); //$NON-NLS-1$
        }

        // Get the dialog_id
        envServices = System.getenv("DIALOG_ID"); //$NON-NLS-1$
        if (envServices != null) {
            dialog_id = envServices;
            UtilityFunctions.logger.info(Messages.getString("WDSBlueMixProxyResource.DIALOG_ACCOUNT_ID_SUCCESS")); //$NON-NLS-1$
        } else {
            UtilityFunctions.logger.error(Messages.getString("WDSBlueMixProxyResource.DIALOG_ACCOUNT_ID_FAIL")); //$NON-NLS-1$
        }

    }

    /**
     * Sets the values of the dialog-specific variables
     */
    private static void useDialogServiceInstance() {
        if (username_dialog != null && password_dialog != null) {
            dialogService = new DialogService();
            dialogService.setUsernameAndPassword(username_dialog, password_dialog);
            dialogService.setEndPoint(wds_base_url);
        }else{
            UtilityFunctions.logger.error(Messages.getString("WDSBlueMixProxyResource.DIALOG_CREDENTIALS_EMPTY"));
       }
    }

    
    
    /**
     * Checks and extracts movie parameters sent by WDS
     * <p>
     * This will extract movie parameters sent by WDS (in the response text) when they're sent.
     * </p>
     * 
     * @param wdsResponseText the textual part of the response sent by WDS
     * @return the JsonObject containing the response from WDS as well as the parameters and their values sent by WDS.
     * @throws org.json.simple.parser.ParseException 
     */
    public JsonObject matchSearchNowPattern(String wdsResponseText) throws org.json.simple.parser.ParseException {

        int idx = wdsResponseText.toLowerCase().indexOf("{\"search_now\":"); //$NON-NLS-1$

        JsonObject result = new JsonObject();
        if (idx != -1) {
            // token exists, parse out some extra chars from dialog.
            String jsonString = wdsResponseText.substring(idx).trim();
            wdsResponseText = wdsResponseText.substring(0, idx - 1).trim();
            if (jsonString.startsWith("\"")) { 
            	jsonString = jsonString.substring(0);
            }
            if (jsonString.endsWith("\"")) { 
            	jsonString = jsonString.substring(0, jsonString.length() - 1);
            }
            
            String trimmedString = jsonString.trim();
            String newline = System.getProperty("line.separator");
    		boolean hasNewline = jsonString.contains(newline);
    		

            JsonParser parser = new JsonParser();
            JsonObject jsonObject = (JsonObject) parser.parse(trimmedString);
            
            System.out.println("DEBUG matchSearchPatternNow: " + jsonObject.toString());
            System.out.println("DEBUG matchSearchPatternNow: wds response is " + jsonString);
            System.out.println("DEBUG matchSearchPatternNow: wds response type is " + jsonString.getClass().getName());
            System.out.println("DEBUG: trimmed String is " + trimmedString);
            System.out.println("DEBUG PersonalityInsightsHelper: " + hasNewline);
            System.out.println(jsonObject.entrySet());
            
            result.add("Params",jsonObject);
    
        }
        
        result.addProperty("WDSMessage", wdsResponseText); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        return result;
    }
    
    

    /**
     * Makes chat conversation with WDS
     * <p>
     * This makes chat conversation with WDS for the provided client id and conversation id, against the user input provided.
     * </p>
     * <p>
     * When WDS has collected all the required movie preferences, it sends a bunch of movie parameters embedded in the text response and signals to discover
     * movies from themoviedb.org. There may be the following kinds of discover movie calls:
     * <ul>
     * <li>New search: First time searching for the given set of parameters
     * <li>Repeat search: Repeat the search with the same parameters (just re-display the results)
     * <li>Previous search: Display the results on the previous page
     * <li>Next search: Display the results on the next page
     * </ul>
     * Depending on the kind of call, profile variables are set in WDS and personalized prompts are retrieved to be sent back to the UI in the payload.
     * </p>
     * 
     * @param conversationId the conversation id for the client id specified
     * @param clientId the client id for the session
     * @param input the user's input
     * @return a response containing either of these two entities- {@code WDSConversationPayload} or {@code ServerErrorPayload}
     */
    @GET
    @Path("/postConversation")
    @Produces(MediaType.APPLICATION_JSON)
    public Response postConversation(@QueryParam("conversationId") String conversationId, @QueryParam("clientId") String clientId,
            @QueryParam("input") String input) throws Exception {
        long lStartTime = System.nanoTime();
        long lEndTime, difference;
        String errorMessage = null, issue = null;
        String wdsMessage = null;
        JsonObject processedText = null;
        String customerEmotion = "neutral";
        //JSONObject processedText = null;
        
        System.out.println("DEBUG WDSBlueMixProxyResource.postConversation: customer conversation turn is " + input);
        
        // Input is null
        if (input == null || input.trim().isEmpty()) {
            errorMessage = Messages.getString("WDSBlueMixProxyResource.SPECIFY_INPUT"); //$NON-NLS-1$
            issue = Messages.getString("WDSBlueMixProxyResource.EMPTY_QUESTION"); //$NON-NLS-1$
            //UtilityFunctions.logger.error(issue);
            return Response.serverError().entity(new ServerErrorPayload(errorMessage, issue)).build();
        }
        
        // Input is not null - the customer has provided some text in their conversation turn
        try {

            // Get the emotions for the customer's conversation turn
        	customerEmotion = getEmotion(input);
        	System.out.println("DEBUG WDSBlueMixProxyResource.postConversation: customer's primary emotion is " + customerEmotion);

        	Map<String, Object> converseParams = createConversationParameterMap(dialog_id,
            		Integer.parseInt(clientId),Integer.parseInt(conversationId),input);
            Conversation conversation = dialogService.converse(converseParams);
            wdsMessage = StringUtils.join(conversation.getResponse(), " ");
            
            System.out.println("DEBUG: response from WDS" + wdsMessage);
            
            
       
            
            
            processedText = matchSearchNowPattern(wdsMessage);
            System.out.println("DEBUG PROCESSED TEXT: " + processedText);
            String wds = processedText.get("WDSMessage").toString();
            String cleanedWds = wds.replace("\"", "");
            WDSConversationPayload conversationPayload = new WDSConversationPayload();
            
            
            if (!processedText.has("Params")) {
 
            	conversationPayload.setClientId(clientId); //$NON-NLS-1$
                conversationPayload.setConversationId(clientId); //$NON-NLS-1$
                conversationPayload.setInput(input); //$NON-NLS-1$
                conversationPayload.setEmotion(customerEmotion); //$NON-NLS-1$
                conversationPayload.setWdsResponse(processedText.get("WDSMessage").getAsString()); 
                
                if (UtilityFunctions.logger.isTraceEnabled()) {
                    // Log the execution time.
                    lEndTime = System.nanoTime();
                    difference = lEndTime - lStartTime;
                    UtilityFunctions.logger.trace("Throughput: " + difference/1000000 + "ms.");
                }
                return Response.ok(conversationPayload, MediaType.APPLICATION_JSON_TYPE).build();
            } 
            
            // Query required
            else {
                // Extract the query parameters from the processed WDS Response
                JsonObject paramsObj = processedText.getAsJsonObject("Params"); 
                System.out.println("DEBUG WDSBlueMixProxyResource: query parameters are " + paramsObj);
                
                // If the query parameters include a clothing store preference variable, the query is a X	
                String prompt;
                List<NameValue> nameValues;
                
                // personality profile query
                 if(paramsObj.has("Twitter_Handle")){
                	String twitterHandle = paramsObj.get("Twitter_Handle").getAsString();
	                System.out.println("DEBUG WDSBlueMixProxyResource: twitter handle is " + twitterHandle);

	                String personalityProfile = "";
	                TwitterAnalyzer twitterAnalyzer = new TwitterAnalyzer();  
            		try {
            			personalityProfile = twitterAnalyzer.getPersonalityProfile(twitterHandle);
            		} 
            		catch (Exception e1) {
            			e1.printStackTrace();
            		}

            		System.out.println("DEBUG WDSBlueMixProxyResource: the personality profile is " + personalityProfile);
            		
            		
            		// Issue updateProfile request to the WDS to update the profile variable - Personality Profile - for the client/dialog
	                // TODO: use a better variable name that nameValues - not descriptive at all
	                nameValues = new ArrayList<NameValue>();
	                nameValues.add(new NameValue("Personality_Profile", personalityProfile));
	                dialogService.updateProfile(dialog_id, Integer.parseInt(clientId), nameValues);
	                System.out.println("DEBUG: profile variable Personality_Profile update request sent to WDS");
	                
	              
	                // Change variable name for prompt - it's not the correct concept - not descriptive enough!!
	                prompt="UPDATE_PERSONALITY_PROFILE";
	                converseParams = createConversationParameterMap(dialog_id,Integer.parseInt(clientId),Integer.parseInt(conversationId),prompt);
	                conversation = dialogService.converse(converseParams);
	                wdsMessage = StringUtils.join(conversation.getResponse(), " ");
	
	                // Build the payload - wdsResponse?
	                conversationPayload.setWdsResponse(wdsMessage);
	                conversationPayload.setClientId(clientId); 
	                conversationPayload.setConversationId(clientId); 
	                conversationPayload.setInput(input); 
	                conversationPayload.setEmotion(customerEmotion); 
	
	                // Removed the logger from here - add it back in later on

	                // Return to UI.
	                return Response.ok(conversationPayload, MediaType.APPLICATION_JSON_TYPE).build();	
            		
            		
            		
                }
            	
                

                // matching clothing stores query
                if(paramsObj.has("Personality_Profile")){
	                System.out.println("DEBUG: query param is Personality_Profile, do a Clothing Store Match query.");

	                String personalityProfile = paramsObj.get("Personality_Profile").toString();
	                System.out.println("DEBUG: personality profile is " + personalityProfile);
	                
	                // Previously found the top matching store
	                String matchingStore = "";
	                matchingStore = new Similarity().getClosestMatchingStore(personalityProfile);
	                System.out.println("DEBUG: matching store from Similarity.getClosestMatchingStore is " + matchingStore);
	                
	                // NEW - find the top five matching stores
	                ArrayList<String> top5Stores = new Similarity().getTopNMatchingStores(personalityProfile, 5);
	                System.out.println(top5Stores);
	                String top5StoresString = StringUtils.join(top5Stores, ',');
	                
	                /*
	                List<StorePayload> stores = new ArrayList<StorePayload>();
	                for(int i=0; i < top5Stores.size(); i++){
	                	StorePayload s = new StorePayload();
	                	s.setName(top5Stores.get(i));
	                	stores.add(i, s);
	                }
	                System.out.println(stores);
	                */
	                
			        
			        
			        
			        // Issue updateProfile request to the WDS to update the profile variable - Clothing_Store_Location - for the client/dialog
	                nameValues = new ArrayList<NameValue>();
	                //nameValues.add(new NameValue("Clothing_Store_Preference", matchingStore)); //$NON-NLS-1$
	                nameValues.add(new NameValue("Clothing_Store_Preference", top5StoresString));
	                dialogService.updateProfile(dialog_id, Integer.parseInt(clientId), nameValues);
	                System.out.println("DEBUG: after call to dialogService");
	                
	              
	                // Send an UPDATE_CLOTHING_STORES request to the WDS
	                // Change variable name for prompt - it's not the correct concept - not descriptive enough!!
	                prompt="UPDATE_CS";
	                converseParams = createConversationParameterMap(dialog_id,Integer.parseInt(clientId),Integer.parseInt(conversationId),prompt);
	                conversation = dialogService.converse(converseParams);
	                wdsMessage = StringUtils.join(conversation.getResponse(), " ");
	
	                // Build the payload - wdsResponse?
	                conversationPayload.setWdsResponse(wdsMessage);
	                conversationPayload.setClientId(clientId); 
	                conversationPayload.setConversationId(clientId); 
	                //conversationPayload.setStores(stores);
	                conversationPayload.setEmotion(customerEmotion);
	                conversationPayload.setInput(input); 
	
	                // Removed the logger from here - add it back in later on

	                // Return to UI.
	                return Response.ok(conversationPayload, MediaType.APPLICATION_JSON_TYPE).build();

                } // END GET MATCHING STORE
	                
	       
	            // Google Maps API Call    
		        if(paramsObj.has("Clothing_Store") && paramsObj.has("Current_Address")){  
		        	String clothingStore = paramsObj.get("Clothing_Store").toString();
		        	String startingAddress = paramsObj.get("Current_Address").toString();
	                System.out.println("DEBUG WDSBlueMixProxyResource: closest clothing store requested. Clothing store preference is " + clothingStore);
	                clothingStore = clothingStore.replace("\"", "");
	                System.out.println("DEBUG WDSBlueMixProxyResource.postConversation: top 5 clothing store string from WDS is " + clothingStore);
	                
	                List<String> top5Stores = new ArrayList<String>(Arrays.asList(clothingStore.split(",")));
	                System.out.println("DEBUG WDSBlueMixProxyResource.postConversation: top 5 clothing store list is " + top5Stores.toString());
	                System.out.println("DEBUG WDSBlueMixProxyResource.postConversation: top5Stores as list to string is " + top5Stores.toString());
	                List<StorePayload> stores = new ArrayList<StorePayload>();
	                for(int i=0; i < top5Stores.size(); i++){
	                	StorePayload s = new StorePayload();
	                	String storeName = top5Stores.get(i);
	                	 Map<String,Object> closestClothingStore = getClosestClothingStoreObject(startingAddress,storeName);
		                String closestStoreAddress = (String)closestClothingStore.get("address");
		                String closestStorePlaceId = (String)closestClothingStore.get("place_id");
		                System.out.println("DEBUG WDSBlueMixProxyResource: closest Clothing Store is located at " + closestStoreAddress);
		                System.out.println("DEBUG WDSBlueMixProxyResource: closest Clothing Store place_id is " + closestStorePlaceId);
		                String distCurrentLocnToClothingStore = getDistance(startingAddress, closestStoreAddress);
		                System.out.println("DEBUG WDSBlueMixProxyResource: distance to the closest Clothing Store is " + distCurrentLocnToClothingStore);

	                	s.setName(storeName);
	                	s.setAddress(closestStoreAddress);
	                	s.setId(closestStorePlaceId);
	                	System.out.println("DEBUG WDSBlueMixProxyResource.postConversation: store is " + storeName + ", id is " + closestStorePlaceId +  " and address is " + closestStoreAddress);
	                	stores.add(i, s);
	                }
	                System.out.println(stores);
	                
	                
	                // Get the closest clothing store - call GoogleMaps api
	                //Map<String,Object> closestClothingStore = getClosestClothingStoreObject(startingAddress,clothingStore);
	                //String closestClothingStoreAddress = (String)closestClothingStore.get("address");
	                //System.out.println("DEBUG WDSBlueMixProxyResource: closest Clothing Store is located at " + closestClothingStoreAddress);
	                //String distCurrentLocnToClothingStore = getDistance(startingAddress, closestClothingStoreAddress);
	                //System.out.println("DEBUG WDSBlueMixProxyResource: distance to the closest Clothing Store is " + distCurrentLocnToClothingStore);
	                
	                
	                
	                /*
	                Map<String,Object> closestGroceryStoreObject = getClosestGroceryStoreAddress(closestClothingStoreAddress);
	                String groceryStoreName = (String)closestGroceryStoreObject.get("name");
	                String groceryStoreAddress = (String)closestGroceryStoreObject.get("address");
	                System.out.println("DEBUG WDSBlueMixProxyResource: closest grocery store is " + groceryStoreName + " and is located at " + groceryStoreAddress);
	                */
	                
	                // Tell WDS to update the profile with the included profile variable values
	                nameValues = new ArrayList<NameValue>();
	                //nameValues.add(new NameValue("Clothing_Store_Location", closestClothingStoreAddress)); //$NON-NLS-1$
	                //nameValues.add(new NameValue("Clothing_Store_Distance", distCurrentLocnToClothingStore));
	                //nameValues.add(new NameValue("Grocery_Store_Name", groceryStoreName));
	                //nameValues.add(new NameValue("Grocery_Store_Address", groceryStoreAddress));
	                nameValues.add(new NameValue("Clothing_Store_Location", "placeholder - fix this"));
	                dialogService.updateProfile(dialog_id, Integer.parseInt(clientId), nameValues);
	                System.out.println("DEBUG WDSBlueMixProxyResource: after call to dialogService");
	                
	              
	                // Send an UPDATE_CLOTHING_STORES request to the WDS
	                // Change variable name for prompt - it's not the correct concept - not descriptive enough!!
	                prompt="UPDATE_CS_LOCATION";
	                converseParams = createConversationParameterMap(dialog_id,Integer.parseInt(clientId),Integer.parseInt(conversationId),prompt);
	                conversation = dialogService.converse(converseParams);
	                wdsMessage = StringUtils.join(conversation.getResponse(), " ");

	                
	                // Build the payload - wdsResponse?
	                conversationPayload.setWdsResponse(wdsMessage);
	                conversationPayload.setClientId(clientId); 
	                conversationPayload.setConversationId(clientId); 
	                System.out.println("DEBUG WDSBlueMixProxyResource: clientId is " + conversationPayload.getClientId());
	                conversationPayload.setInput(input); 
	                //conversationPayload.setStores(storesList);
	                conversationPayload.setStores(stores);
	                conversationPayload.setEmotion(customerEmotion);
	                System.out.println("DEBUG WDSBlueMixProxyResource: 1st store is " + conversationPayload.getStores().get(0).getName());
	
	                // Removed the logger from here - add it back in later on

	                // Return to UI.
	                return Response.ok(conversationPayload, MediaType.APPLICATION_JSON_TYPE).build();
                }// END Clothing_Store_Preference query processing and update
            }
                

        } 
        catch (IllegalStateException e) {
            errorMessage = Messages.getString("WDSBlueMixProxyResource.API_CALL_NOT_EXECUTED"); //$NON-NLS-1$
            issue = Messages.getString("WDSBlueMixProxyResource.ILLEGAL_STATE_GET_RESPONSE"); //$NON-NLS-1$
            UtilityFunctions.logger.error(issue, e);
        } 
  
        return Response.serverError().entity(new ServerErrorPayload(errorMessage, issue)).build();
    }
    

    @GET
    @Path("/getSelectedStoreDetails")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSelectedStoreDetails(@QueryParam("clientId") String clientId, @QueryParam("conversationId") String conversationId,
            @QueryParam("name") String name) throws ParseException,IOException, HttpException, WatsonTheatersException {
    	System.out.println("DEBUG WDSBlueMixProxyResource.getSelectedStoreDetails: called with name " + name);
    	
        String errorMessage = Messages.getString("WDSBlueMixProxyResource.WDS_API_CALL_NOT_EXECUTED"); //$NON-NLS-1$
        String issue = null;
        WDSConversationPayload conversationPayload = new WDSConversationPayload();
        try {

            StorePayload store = new StorePayload();
            store.setName(name);

            // Get customer's current address - persisted on the Watson Dialog Server
            String currentAddress = null;
        	List<NameValue> test = dialogService.getProfile(dialog_id, Integer.parseInt(clientId));
        	System.out.println("DEBUG WDSBlueMixProxyResource.getSelectedStoreDetails: profile variables are" + test);
        	for (ListIterator<NameValue> iter = test.listIterator(); iter.hasNext(); ) {
        		NameValue element = iter.next();
        		String elementName = element.getName();
        		String elementValue = element.getValue();
        		System.out.println("name: " + elementName + ", value: " + elementValue);
        		if (elementName.equals("Current_Address")) {
        			currentAddress = elementValue;
        		}
        	}
        	System.out.println("DEBUG WDSBlueMixProxyResource.getSelectedStoreDetails: current address is " + currentAddress);
            //store.setAddress(currentAddress);
            
            
            // Get the address of the closest clothing store
            Map<String,Object> closestClothingStore = getClosestClothingStoreObject(currentAddress,name);
            String closestStoreAddress = (String)closestClothingStore.get("address");
            System.out.println("DEBUG WDSBlueMixProxyResource: closest Clothing Store is located at " + closestStoreAddress);
            
            //String distCurrentLocnToClothingStore = getDistance(currentAddress, closestClothingStoreAddress);
            //System.out.println("DEBUG WDSBlueMixProxyResource: distance to the closest Clothing Store is " + distCurrentLocnToClothingStore);
            
            
            //store.setMapURL("https://www.google.com/maps/embed/v1/search?q=j+crew+near+505+Cypress+Point+Drive+Mountain+View&key=AIzaSyB1RD2gilBuJjZPQP500vCZPMoDqGfBav8");
            //store.setId("1");
            
            // Create and set the Google mapurl that provides directions from the currentAddress to the closestStoreAddress
            String mapUrl = "https://www.google.com/maps/embed/v1/directions?" 
            		+ "key=" + GOOGLE_MAPS_EMBED_API_KEY  
            		+ "&origin=" + URLEncoder.encode(currentAddress, "UTF-8")
            		+ "&destination=" + URLEncoder.encode(store.getName(), "UTF-8") + " @"+ URLEncoder.encode(closestStoreAddress, "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
            		//+ "&destination=" + URLEncoder.encode(closestStoreAddress, "UTF-8"); //$NON-NLS-1$ //$NON-NLS-2$
            store.setMapUrl(mapUrl);
            System.out.println("DEBUG WDSBlueMixProxyResource.getSelectedStoreDetails: mapurl is " + store.getMapUrl());
            

            // Set the profile variable for WDS.
            List<NameValue> nameValues = new ArrayList<NameValue>();
            nameValues = new ArrayList<NameValue>();
            nameValues.add(new NameValue("Selected_Store", URLEncoder.encode(name, "UTF-8"))); //$NON-NLS-1$
            dialogService.updateProfile(dialog_id, Integer.parseInt(clientId), nameValues);
            System.out.println("DEBUG WDSBlueMixProxyResource.getSelectedStoreDetails: profile variable Personality_Profile update request sent to WDS");
                

            // Get the personalized prompt.
            Map<String, Object> converseParams = new HashMap<String, Object>();
            converseParams.put("dialog_id", dialog_id);
            converseParams.put("client_id", Integer.parseInt(clientId));
            converseParams.put("conversation_id", Integer.parseInt(conversationId));
            converseParams.put("input", personalized_prompt_store_selected);
            Conversation conversation = dialogService.converse(converseParams);
            String wdsMessage = StringUtils.join(conversation.getResponse(), " ");

            // Add the wds personalized prompt to the MoviesPayload and return.
            //List<StorePayload> storeList = new ArrayList<StorePayload>();
            // storeList.add(store);
            //conversationPayload.setStores(storeList);
            List<StorePayload> stores = new ArrayList<StorePayload>();
            stores.add(store);
            conversationPayload.setStores(stores);
            System.out.println("DEBUG WDSBlueMixProxyResource.getSelectedStoreDetails: stores added to the payload.  First store is " + conversationPayload.getStores().get(0).getName());
            conversationPayload.setWdsResponse(wdsMessage);
            return Response.ok(conversationPayload, MediaType.APPLICATION_JSON_TYPE).build();

        } catch (IllegalStateException | URISyntaxException e) {
            issue = Messages.getString("WDSBlueMixProxyResource.ILLEGAL_STATE_EXCEPTION_GET_RESPONSE"); //$NON-NLS-1$
            UtilityFunctions.logger.error(issue, e);
        }
        return Response.serverError().entity(new ServerErrorPayload(errorMessage, issue)).build();
    }   
    
    
 private String TwitterAnalyzer(String string) {
		// TODO Auto-generated method stub
		return null;
	}


 
 
 
 private static Double getAnger(String text) throws ClientProtocolException, IOException, org.json.simple.parser.ParseException, URISyntaxException {

	Double anger = null;
	WatsonEmotionServiceProxyResource wesObj = new WatsonEmotionServiceProxyResource();
	System.out.println("DEBUG WDSBlueMixProxyResource: text to pass to Emotion Service is " + text);
	
	if(text.trim().isEmpty()){
		System.out.println("DEBUG WDSBlueMixProxyResource: text provided to getAnger function is empty");
	}else{
		anger = (Double)wesObj.getAnger(text);
		System.out.println("DEBUG WDSBlueMixProxyResource: anger level is " + anger.toString());
	}

 	return anger;
	}
 
 private static HashMap<String,Double> getEmotionMap(String text) throws ClientProtocolException, IOException, org.json.simple.parser.ParseException, URISyntaxException {

	HashMap<String,Double> emotionMap = new HashMap<String,Double>();
	WatsonEmotionServiceProxyResource wesObj = new WatsonEmotionServiceProxyResource();
	System.out.println("DEBUG WDSBlueMixProxyResource.getEmotionMap: text to pass to emotion service is " + text);
	
	if(text.trim().isEmpty()){
		emotionMap = null;
		System.out.println("DEBUG WDSBlueMixProxyResource.getEmotionMap: text is empty");
	}else{
		emotionMap = (HashMap<String,Double>)wesObj.getEmotionMap(text);
		System.out.println("DEBUG WDSBlueMixProxyResource: emotion map is " + emotionMap.toString());
	}
	
 	return emotionMap;
	}
 
 private static String getEmotion(String text) throws ClientProtocolException, IOException, org.json.simple.parser.ParseException, URISyntaxException {

		String emotion = "neutral";
		Double maxValue = null;
		HashMap<String,Double> emotionMap = getEmotionMap(text);
		
		Entry<String,Double> maxEntry = null;

		for(Entry<String,Double> entry : emotionMap.entrySet()) {
		    if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
		        maxEntry = entry;
		    }
		}
		if(maxEntry.getValue() >= 0.5){
			emotion = maxEntry.getKey();
		}
		System.out.println("DEBUG WDXBlueMixProxyResource.getEmotion: emotion is " + emotion);
 		return emotion;
}
 
 
/*private String getClosestClothingStoreAddress(String fromAddress, String clothingStore, String distance) throws ClientProtocolException, IOException, org.json.simple.parser.ParseException, URISyntaxException {

		GoogleMapsProxyResource gmObj = new GoogleMapsProxyResource();
		System.out.println("DEBUG WDSBlueMixProxyResource: client starting address address is " + fromAddress);
		
	    String closestClothingStoreAddress = gmObj.getClosestLocation(fromAddress, "clothing_store", clothingStore);

	    System.out.println("DEBUG WDSBlueMixProxyResource: closest clothing store address is " + closestClothingStoreAddress);

	 	return closestClothingStoreAddress;
	}
*/
private Map<String,Object> getClosestClothingStoreObject(String fromAddress, String clothingStore) throws ClientProtocolException, IOException, org.json.simple.parser.ParseException, URISyntaxException {

	GoogleMapsProxyResource gmObj = new GoogleMapsProxyResource();
	System.out.println("DEBUG WDSBlueMixProxyResource: client starting address address is " + fromAddress);
	
    Map<String,Object> closestClothingStoreObject = gmObj.getClosestLocationObject(fromAddress,"clothing_store", clothingStore);

    System.out.println("DEBUG WDSBlueMixProxyResource: closest clothing store address is " + closestClothingStoreObject);

 	return closestClothingStoreObject;
}

private String getDistance(String fromAddress, String toAddress) throws ClientProtocolException, IOException, org.json.simple.parser.ParseException, URISyntaxException {

	GoogleMapsProxyResource gmObj = new GoogleMapsProxyResource();
	System.out.println("DEBUG WDSBlueMixProxyResource: client starting address address is " + fromAddress);
	
    String distance = gmObj.getDistance(fromAddress,toAddress);

    System.out.println("DEBUG WDSBlueMixProxyResource: distance to closest clothing store is  " + distance);

 	return distance;
}


private Map<String,Object> getClosestGroceryStoreAddress(String fromAddress) throws ClientProtocolException, IOException, org.json.simple.parser.ParseException, URISyntaxException {

	GoogleMapsProxyResource gmObj = new GoogleMapsProxyResource();
	System.out.println("DEBUG WDSBlueMixProxyResource: client starting address address is " + fromAddress);
	
    Map<String, Object> closestLocationObject = gmObj.getClosestLocationObject(fromAddress, "grocery_or_supermarket");

    System.out.println("DEBUG WDSBlueMixProxyResource: closest clothing store address is " + closestLocationObject.toString());

 	return closestLocationObject;
}

private void createConversationParameters(String dialog_id2, int parseInt) {
		// TODO Auto-generated method stub
		
	}

	// Create the map containing the parameters required by the Dialog Service
    /**
     * Creates a map of the conversation parameters required to converse with the Dialog Service 
     * 
     *  @author - April Webster (Jan 13, 2016) awebster@us.ibm.com
     *  @return a map of the conversation parameters required by the Dialog Service for conversing.
     *  @param dialog_id - the id associated with a dialog provided by the Dialog Service
     *  @param client_id - the id associated with the client conversing with dialog_id
     *  @param conversation_id - the id of the specific conversation client_id is having with dialog_id (there can be more than one)
     *  @param input - the string representing the client's conversation turn
     */
    public Map<String,Object> createConversationParameterMap (String dialog_id, Integer client_id, Integer conversation_id, Object input){
		Map<String, Object> converseParams = new HashMap<String, Object>();
	    converseParams.put("dialog_id", dialog_id);
	    converseParams.put("client_id", client_id);
	    converseParams.put("conversation_id", conversation_id);
	    converseParams.put("input", input); //Client's conversation turn 
	    return converseParams;
    }
    
    
 // Create the map containing the parameters required by the Dialog Service
    /**
     * Creates a map of the conversation parameters required to converse with the Dialog Service 
     * 
     *  @author - April Webster (Jan 13, 2016) awebster@us.ibm.com
     *  @return a map of the conversation parameters required by the Dialog Service for conversing.
     *  @param dialog_id - the id associated with a dialog provided by the Dialog Service
     *  @param client_id - the id associated with the client conversing with dialog_id
     *  @param conversation_id - the id of the specific conversation client_id is having with dialog_id (there can be more than one)
     *  @param input - the string representing the client's conversation turn
     */
    public WDSConversationPayload createConversationPayload (String clientId, String input, String wdsResponse){
    	WDSConversationPayload conversationPayload = new WDSConversationPayload();
		conversationPayload.setClientId(clientId); //$NON-NLS-1$
        conversationPayload.setConversationId(clientId); //$NON-NLS-1$
        conversationPayload.setInput(input); //$NON-NLS-1$
        conversationPayload.setWdsResponse(wdsResponse); //$NON-NLS-1$
        
	    return conversationPayload;
    }
    
    
    
    

    
    
    
    
    /**
     * Returns selected movie details
     * <p>
     * This extracts the details of the movie specified. It uses themoviedb.org API to populate movie details in {@link MoviePayload}.
     * </p>
     * 
     * @param clientId the client id for the session
     * @param conversationId the conversation id for the client id specified
     * @param movieName the movie name
     * @param movieId the movie id
     * @return a response containing either of these two entities- {@code WDSConversationPayload} or {@code ServerErrorPayload}
     */
    @GET
    @Path("/getSelectedMovieDetails")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSelectedMovieDetails(@QueryParam("clientId") String clientId, @QueryParam("conversationId") String conversationId,
            @QueryParam("movieName") String movieName, @QueryParam("movieId") String movieId) throws IOException, HttpException, WatsonTheatersException {

        String errorMessage = Messages.getString("WDSBlueMixProxyResource.WDS_API_CALL_NOT_EXECUTED"); //$NON-NLS-1$
        String issue = null;
        WDSConversationPayload conversationPayload = new WDSConversationPayload();
        try {
            // Get movie info from TMDB.
            SearchTheMovieDbProxyResource tmdb = new SearchTheMovieDbProxyResource();
            Response tmdbResponse = tmdb.getMovieDetails(movieId, movieName);
            MoviePayload movie = (MoviePayload) tmdbResponse.getEntity();

            // Set the profile variable for WDS.
            Map<String, String> profile = new HashMap<>();
            profile.put("Selected_Movie", URLEncoder.encode(movieName, "UTF-8")); //$NON-NLS-1$ //$NON-NLS-2$
            profile.put("Popularity_Score", movie.getPopularity().toString()); //$NON-NLS-1$
            dialogService.updateProfile(dialog_id, new Integer(clientId), (List<NameValue>) profile);

            // Get the personalized prompt.
            Map<String, Object> converseParams = new HashMap<String, Object>();
            converseParams.put("dialog_id", dialog_id);
            converseParams.put("client_id", Integer.parseInt(clientId));
            converseParams.put("conversation_id", Integer.parseInt(conversationId));
            converseParams.put("input", personalized_prompt_movie_selected);
            Conversation conversation = dialogService.converse(converseParams);
            String wdsMessage = StringUtils.join(conversation.getResponse(), " ");

            // Add the wds personalized prompt to the MoviesPayload and return.
            List<MoviePayload> movieList = new ArrayList<MoviePayload>();
            movieList.add(movie);
            conversationPayload.setMovies(movieList);
            conversationPayload.setWdsResponse(wdsMessage);
            if (UtilityFunctions.logger.isTraceEnabled()) {
                UtilityFunctions.logger
                        .trace(Messages.getString("WDSBlueMixProxyResource.MOVIE_NAME") + movieName + Messages.getString("WDSBlueMixProxyResource.POPULARITY") + movie.getPopularity().toString()); //$NON-NLS-1$ //$NON-NLS-2$
                UtilityFunctions.logger.trace(Messages.getString("WDSBlueMixProxyResource.WDS_PROMPT_SELECTED_MOVIE") + wdsMessage); //$NON-NLS-1$
            }
            return Response.ok(conversationPayload, MediaType.APPLICATION_JSON_TYPE).build();

        } catch (IllegalStateException e) {
            issue = Messages.getString("WDSBlueMixProxyResource.ILLEGAL_STATE_EXCEPTION_GET_RESPONSE"); //$NON-NLS-1$
            UtilityFunctions.logger.error(issue, e);
        }
        return Response.serverError().entity(new ServerErrorPayload(errorMessage, issue)).build();
    }
    
    
    @GET
    @Path("/getEmotion")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@QueryParam("clientId") String clientId, @QueryParam("conversationId") String conversationId,
            @QueryParam("text") String text) throws IOException, HttpException, WatsonTheatersException, ParseException, URISyntaxException {

    	System.out.println("DEBUG WDSBlueMixProxyResource.getEmotion: called with text " + text);
    	
    	String emotion = getEmotion(text);
    	System.out.println("DEBUG WDSBlueMixProxyResource.getEmotion: emotion is " + emotion);
    	
    	
    	
        String errorMessage = Messages.getString("WDSBlueMixProxyResource.WDS_API_CALL_NOT_EXECUTED"); //$NON-NLS-1$
        String issue = null;
        WDSConversationPayload conversationPayload = new WDSConversationPayload();
        try {
        	Map<String, Object> converseParams = new HashMap<String, Object>();
            converseParams.put("dialog_id", dialog_id);
            converseParams.put("client_id", Integer.parseInt(clientId));
            converseParams.put("conversation_id", Integer.parseInt(conversationId));
            conversationPayload.setEmotion(emotion);
            return Response.ok(conversationPayload, MediaType.APPLICATION_JSON_TYPE).build();

        } catch (IllegalStateException e) {
            issue = Messages.getString("WDSBlueMixProxyResource.ILLEGAL_STATE_EXCEPTION_GET_RESPONSE"); //$NON-NLS-1$
            UtilityFunctions.logger.error(issue, e);
        }
        return Response.serverError().entity(new ServerErrorPayload(errorMessage, issue)).build();
    }
    
    
    

    /**
     * Initializes chat with WDS This initiates the chat with WDS by requesting for a client id and conversation id(to be used in subsequent API calls) and a
     * response message to be displayed to the user. If it's a returning user, it sets the First_Time profile variable to "No" so that the user is not taken
     * through the hand-holding process.
     * 
     * @param firstTimeUser specifies if it's a new user or a returning user(true/false). If it is a returning user WDS is notified via profile var.
     * 
     * @return a response containing either of these two entities- {@code WDSConversationPayload} or {@code ServerErrorPayload}
     */
    @GET
    @Path("/initChat")
    @Produces(MediaType.APPLICATION_JSON)
    public Response startConversation(@QueryParam("firstTimeUser") boolean firstTimeUser) {
        Conversation conversation = dialogService.createConversation(dialog_id);
        if (!firstTimeUser) {
            List<NameValue> nameValues = new ArrayList<NameValue>();
            nameValues.add(new NameValue("First_Time", "No"));
            dialogService.updateProfile(dialog_id, conversation.getClientId(), nameValues);
        }
        
        System.out.println("DEBUG WDSBlueMixProxyResource.initChat: clientID = " + conversation.getClientId());
        System.out.println("DEBUG WDSBlueMixProxyResource.initChat: conversationID = " + conversation.getId());
        System.out.println("DEBUG WDSBlueMixProxyResource.initChat: input = " + conversation.getInput());
        WDSConversationPayload conversationPayload = new WDSConversationPayload();
        conversationPayload.setClientId(Integer.toString(conversation.getClientId())); //$NON-NLS-1$
        conversationPayload.setConversationId(Integer.toString(conversation.getId())); //$NON-NLS-1$
        conversationPayload.setInput(conversation.getInput()); //$NON-NLS-1$
        conversationPayload.setWdsResponse(StringUtils.join(conversation.getResponse(), " "));
        return Response.ok(conversationPayload, MediaType.APPLICATION_JSON_TYPE).build();
    }
}