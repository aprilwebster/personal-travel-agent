package com.ibm.watson.travelagentapp.webservices.googlemaps;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



public class GoogleMapsProxyResource{
	
	private static final String GOOGLE_MAPS_BASE_URL = "maps.googleapis.com/maps/api/";
    private static final String GEOCODE_PATH = "geocode/";
    private static final String PLACE_NEARBY_PATH = "place/nearbysearch/";
    private static final String DISTANCE_MATRIX = "distancematrix/";
    private static final String RESPONSE_TYPE = "json";
    private static final String SEARCH_NOW_KEY = "AIzaSyA1--v45ZIXg0xy5xscQmgW7u-cwBWEFQE";
    private static final String DISTANCE_MATRIX_KEY = "AIzaSyB1RD2gilBuJjZPQP500vCZPMoDqGfBav8";
    private static final Boolean DEBUG = true;
    
    private static final String GOOGLE_API_STATUS_OK = "OK";
    
    // fields from the Google Search Nearby API Response
    private static final String STATUS = "status";
    private static final String RESULTS = "results";
    private static final String GEOMETRY = "geometry";
    private static final String LOCATION = "location";
    private static final String ADDRESS = "vicinity";
    private static final String LATITUDE = "lat";
    private static final String LONGITUDE = "lng";
    private static final String OPENING_HOURS = "opening_hours";
    private static final String STORE_NAME = "name";
    private static final String PLACE_ID = "place_id";
    private static final String OPEN_NOW = "open_now";
    private static final String WEEKDAY_HOURS = "weekday_text";
    
    private static final String IMPERIAL = "imperial";
    private static final String ROWS = "rows";
    private static final String ELEMENTS = "elements";
    private static final String DISTANCE = "distance";
    private static final String TEXT = "text";
  
    
	/*public static void main(String[] args) throws IOException, ParseException, URISyntaxException{
			
			String address = args[1];
			System.out.println("address is " + address);
	
			Map<String, String> latLong = new HashMap<String,String>();
			latLong = getLatLong(address);

		
	}*/
	
	public static Map<String,String> getLatLong(String address) throws ClientProtocolException, IOException, ParseException, URISyntaxException{
		
		// Build the URI for Google Search Nearby
		Hashtable<String, String> uriParamsHash = new Hashtable<String, String>();
		uriParamsHash.put("address", address);
		URI uri = buildUriStringFromParamsHash("https", GOOGLE_MAPS_BASE_URL, GEOCODE_PATH + RESPONSE_TYPE, uriParamsHash,null);	
		
		
		
		//URI uri = getURI("https", GOOGLE_MAPS_BASE_URL, GEOCODE_PATH + RESPONSE_TYPE, address);	
		HttpResponse response = httpGet(uri);
	    JSONObject location = getLocation(response);
	    String lat = getLatitude(location);
	    String lng = getLongitude(location);
	    
	    //System.out.println(lat + ", " + lng);
	    
	    Map<String,String> output = new HashMap<String,String>();
	    output.put("lat", lat);
	    output.put("lng", lng);
	    
	    return output;

	}
	
	
/*	public static String getClosestLocation(String searchAddress, String radius,
		String types, String locationName) throws ClientProtocolException, IOException, ParseException, URISyntaxException{
		
		// Geocode the address that will be used to find the specified type of closest location
		Map<String,String> latLong = getLatLong(searchAddress);
		String latitude = latLong.get("lat");
		String longitude = latLong.get("lng");
		
		if(DEBUG){
			System.out.println("DEBUG GoogleMapsProxyResource: search address is " + searchAddress);
		}
		
		// Build the URI for Google Search Nearby
		Hashtable<String, String> uriParamsHash = new Hashtable<String, String>();
		uriParamsHash.put("location", latitude + "," + longitude);
		uriParamsHash.put("types", types);
		uriParamsHash.put("name", locationName);
		uriParamsHash.put("rankby", "distance");
		URI uri = buildUriStringFromParamsHash("https", GOOGLE_MAPS_BASE_URL, PLACE_NEARBY_PATH + RESPONSE_TYPE, uriParamsHash,SEARCH_NOW_KEY);	
		
		// Get the response from the http Get
		HttpResponse response = httpGet(uri);
	    String strResponse = EntityUtils.toString(response.getEntity());
	    JSONObject jsonResponse = (JSONObject) (new JSONParser()).parse(strResponse);
	    
	    if(DEBUG){
	    	System.out.println("DEBUG GoogleMapsProxyResource: the http response is " + strResponse);
	    	System.out.println("DEBUG GoogleMapsProxyResource: the string response is " + strResponse);
	    	System.out.println("DEBUG GoogleMapsProxyResource: the json response is " + jsonResponse);
	    }
 	
	    // Process the response if the status is 
	    String status = (String)jsonResponse.get(STATUS);
	    System.out.println("DEBUG GoogleMapsProxyResource: the status is " + status);
	    
	    String address = null;
	    
	    // If the status code in the response object from the Google Maps Search Nearby query is OK
	    // process the response and pull out the necessary data
	    if(status.equals(GOOGLE_API_STATUS_OK)){
	    	JSONObject results = (JSONObject) ((JSONArray) jsonResponse.get((Object) RESULTS)).get(0);
		    JSONObject geometry = (JSONObject) results.get((Object) GEOMETRY);
		    JSONObject location = (JSONObject) geometry.get((Object) LOCATION);
		    address = (String) results.get((Object) ADDRESS);
		    //JSONObject opening_hours = (JSONObject) results.get((Object) OPENING_HOURS);
		    //Boolean open_now = (Boolean) opening_hours.get((Object) OPEN_NOW);
		    
		    if(DEBUG){
				System.out.println("DEBUG GoogleMapsProxyResource: location is " + location.toString());
				System.out.println("DEBUG GoogleMapsProxyResource: address is " + address.toString());
				//System.out.println("DEBUG GoogleMapsProxyResource: opening hours is " + opening_hours.toString());
				//System.out.println("DEBUG GoogleMapsProxyResource: open now is " + open_now.toString());
		    }
	    }
	    
	    return address;

	}
	
	public static String getClosestLocation(String searchAddress,
			String types) throws ClientProtocolException, IOException, ParseException, URISyntaxException{
			Hashtable<String, String> uriParamsHash = new Hashtable<String, String>();
			
			System.out.println("DEBUG GoogleMapsProxyResource: search address is " + searchAddress);
			Map<String,String> latLong = getLatLong(searchAddress);
			String latitude = latLong.get("lat");
			String longitude = latLong.get("lng");
			
			uriParamsHash.put("location", latitude + "," + longitude);
			//uriParamsHash.put("radius", radius);
			uriParamsHash.put("types", types);
			uriParamsHash.put("rankby", "distance");
			
			URI uri = buildUriStringFromParamsHash("https", GOOGLE_MAPS_BASE_URL, PLACE_NEARBY_PATH + RESPONSE_TYPE, uriParamsHash,SEARCH_NOW_KEY);	
			HttpResponse response = httpGet(uri);
			System.out.println("DEBUG GoogleMapsProxyResource: the response is " + response);
			
			HttpEntity entity = response.getEntity();
		    String strResponse = EntityUtils.toString(entity);
		    System.out.println("DEBUG GoogleMapsProxyResource: the response entity is " + strResponse);

		    JSONParser parser=new JSONParser();
		    Object p = parser.parse(strResponse);
		    
		    JSONObject jsonResponse =(JSONObject)p;
		    Set keys = jsonResponse.keySet();

	 	
		    String status = (String)jsonResponse.get(STATUS);
		    
		    if(status.equals("ZERO_RESULTS")){
		    	uriParamsHash.put("location", latitude + "," + longitude);
		    	uriParamsHash.put("rankby", "distance");
				uriParamsHash.put("types", types);

				
				uri = buildUriStringFromParamsHash("https", GOOGLE_MAPS_BASE_URL, PLACE_NEARBY_PATH + RESPONSE_TYPE, uriParamsHash,SEARCH_NOW_KEY);	
				response = httpGet(uri);
				System.out.println("DEBUG GoogleMapsProxyResource: the response is " + response);
				
				entity = response.getEntity();
			    strResponse = EntityUtils.toString(entity);
			    System.out.println("DEBUG GoogleMapsProxyResource: the response entity is " + strResponse);
		    }
		    System.out.println("DEBUG GoogleMapsProxyResource: the status is " + status);
		    
		    
		    
		    JSONObject results = (JSONObject) ((JSONArray) jsonResponse.get((Object) RESULTS)).get(0);
		    JSONObject geometry = (JSONObject) results.get((Object) GEOMETRY);
		    JSONObject location = (JSONObject) geometry.get((Object) LOCATION);
		    String address = (String) results.get((Object) ADDRESS);
		    JSONObject opening_hours = (JSONObject) results.get((Object) OPENING_HOURS);
		    Boolean open_now = (Boolean) opening_hours.get((Object) OPEN_NOW);
		    
		    if(DEBUG){
				System.out.println("DEBUG GoogleMapsProxyResource: location is " + location.toString());
				System.out.println("DEBUG GoogleMapsProxyResource: address is " + address.toString());
				System.out.println("DEBUG GoogleMapsProxyResource: opening hours is " + opening_hours.toString());
				System.out.println("DEBUG GoogleMapsProxyResource: open now is " + open_now.toString());
		    }
		    return address;

		}*/
	
	//https://maps.googleapis.com/maps/api/distancematrix/xml?origins=Vancouver+BC&destinations=San+Francisco&units=imperial&key=AIzaSyB1RD2gilBuJjZPQP500vCZPMoDqGfBav8
	public static String getDistance(String origin, String destination) throws URISyntaxException, ClientProtocolException, IOException, ParseException{
		
		Hashtable<String, String> uriParamsHash = new Hashtable<String, String>();
		uriParamsHash.put("units", IMPERIAL);
		uriParamsHash.put("origins", origin);
		uriParamsHash.put("destinations", destination);
		URI uri = buildUriStringFromParamsHash("https", GOOGLE_MAPS_BASE_URL, DISTANCE_MATRIX + RESPONSE_TYPE, uriParamsHash,DISTANCE_MATRIX_KEY);	
		
		// Get the response from the http Get
		HttpResponse response = httpGet(uri);
	    String strResponse = EntityUtils.toString(response.getEntity());
	    JSONObject jsonResponse = (JSONObject) (new JSONParser()).parse(strResponse);
	    
	    if(DEBUG){
	    	System.out.println("DEBUG GoogleMapsProxyResource: the http response is " + strResponse);
	    	System.out.println("DEBUG GoogleMapsProxyResource: the string response is " + strResponse);
	    	System.out.println("DEBUG GoogleMapsProxyResource: the json response is " + jsonResponse);
	    }
 	
	    // Process the response if the status is 
	    String status = (String)jsonResponse.get(STATUS);
	    System.out.println("DEBUG GoogleMapsProxyResource: the status is " + status);
	    
	    String dist = "";
	    
	    // If the status code in the response object from the Google Maps Search Nearby query is OK
	    // process the response and pull out the necessary data
	    if(status.equals(GOOGLE_API_STATUS_OK)){
	    	JSONObject firstRow = (JSONObject) ((JSONArray) jsonResponse.get((Object) ROWS)).get(0);
	    	//JSONObject rows = (JSONObject) jsonResponse.get((Object) ROWS);
	    	System.out.println("DEBUG GoogleMapsProxyResource - get distance: the row is " + firstRow.toString());
	    	JSONObject elements = (JSONObject) ((JSONArray) firstRow.get((Object) ELEMENTS)).get(0);
	    	System.out.println("DEBUG GoogleMapsProxyResource - get distance: the elements is " + firstRow.toString());
		    JSONObject distance = (JSONObject) elements.get((Object) DISTANCE);
		    System.out.println("DEBUG GoogleMapsProxyResource - get distance: the distance object is " + distance.toString());
		    dist = (String) distance.get((Object) TEXT);
		    System.out.println("DEBUG GoogleMapsProxyResource - get distance: the distance is " + dist);
   
		    if(DEBUG){
				//System.out.println("DEBUG GoogleMapsProxyResource: distance between " + origin + " and " + destination + " is " + text);
		    }
	    }
		     
	    return dist;
		
	}

	
 	public static Map<String,Object> getClosestLocationObject(String searchAddress, String type, String locationName) 
			throws ClientProtocolException, IOException, ParseException, URISyntaxException{
			
		// Geocode the address that will be used to find the specified type of closest location
		Map<String,String> latLong = getLatLong(searchAddress);
		String latitude = latLong.get("lat");
		String longitude = latLong.get("lng");
		
		if(DEBUG){
			System.out.println("DEBUG GoogleMapsProxyResource: search address is " + searchAddress);
		}
		
		// Build the URI for Google Search Nearby
		Hashtable<String, String> uriParamsHash = new Hashtable<String, String>();
		uriParamsHash.put("location", latitude + "," + longitude);
		uriParamsHash.put("type", type);
		uriParamsHash.put("name", locationName);
		uriParamsHash.put("rankby", "distance");
		URI uri = buildUriStringFromParamsHash("https", GOOGLE_MAPS_BASE_URL, PLACE_NEARBY_PATH + RESPONSE_TYPE, uriParamsHash,SEARCH_NOW_KEY);	
		
		// Get the response from the http Get
		HttpResponse response = httpGet(uri);
	    String strResponse = EntityUtils.toString(response.getEntity());
	    JSONObject jsonResponse = (JSONObject) (new JSONParser()).parse(strResponse);
	    
	    if(DEBUG){
	    	System.out.println("DEBUG GoogleMapsProxyResource: the http response is " + strResponse);
	    	System.out.println("DEBUG GoogleMapsProxyResource: the string response is " + strResponse);
	    	System.out.println("DEBUG GoogleMapsProxyResource: the json response is " + jsonResponse);
	    }
 	
	    // Process the response if the status is 
	    String status = (String)jsonResponse.get(STATUS);
	    System.out.println("DEBUG GoogleMapsProxyResource: the status is " + status);
	    
	    Map<String,Object> output = new HashMap<String,Object>();
	    
	    
	    // If the status code in the response object from the Google Maps Search Nearby query is OK
	    // process the response and pull out the necessary data
	    if(status.equals(GOOGLE_API_STATUS_OK)){
	    	JSONObject results = (JSONObject) ((JSONArray) jsonResponse.get((Object) RESULTS)).get(0);
		    JSONObject geometry = (JSONObject) results.get((Object) GEOMETRY);
		    JSONObject location = (JSONObject) geometry.get((Object) LOCATION);
		    Double lat = (Double)location.get((Object)LATITUDE);
		    Double lng = (Double)location.get((Object)LONGITUDE);
		    String address = (String) results.get((Object) ADDRESS);
		    String name = (String) results.get((Object) STORE_NAME);
		    String place_id = (String) results.get((Object) PLACE_ID);
		    //JSONObject opening_hours = (JSONObject) results.get((Object) OPENING_HOURS);
		    //Boolean open_now = (Boolean) opening_hours.get((Object) OPEN_NOW);
		    
		    output.put("lat", lat);
		    output.put("lng", lng);
		    output.put("address", address);
		    output.put("name", name);
		    output.put("place_id", place_id);
		    
		    if(DEBUG){
				System.out.println("DEBUG GoogleMapsProxyResource: location is " + location.toString());
				System.out.println("DEBUG GoogleMapsProxyResource: address is " + address.toString());
				System.out.println("DEBUG GoogleMapsProxyResource: place id is " + place_id);
				//System.out.println("DEBUG GoogleMapsProxyResource: opening hours is " + opening_hours.toString());
				//System.out.println("DEBUG GoogleMapsProxyResource: open now is " + open_now.toString());
		    }else{
		    	output = null;
		    }
	    }
		     
	    return output;
	}
	
	public static Map<String,Object> getClosestLocationObject(String searchAddress,String type) 
			throws ClientProtocolException, IOException, ParseException, URISyntaxException{
			
			// Geocode the address that will be used to find the specified type of closest location
			Map<String,String> latLong = getLatLong(searchAddress);
			String latitude = latLong.get("lat");
			String longitude = latLong.get("lng");
			
			if(DEBUG){
				System.out.println("DEBUG GoogleMapsProxyResource: search address is " + searchAddress);
			}
			
			// Build the URI for Google Search Nearby
			Hashtable<String, String> uriParamsHash = new Hashtable<String, String>();
			uriParamsHash.put("location", latitude + "," + longitude);
			uriParamsHash.put("type", type);
			uriParamsHash.put("rankby", "distance");
			URI uri = buildUriStringFromParamsHash("https", GOOGLE_MAPS_BASE_URL, PLACE_NEARBY_PATH + RESPONSE_TYPE, uriParamsHash,SEARCH_NOW_KEY);	
			
			// Get the response from the http Get
			HttpResponse response = httpGet(uri);
		    String strResponse = EntityUtils.toString(response.getEntity());
		    JSONObject jsonResponse = (JSONObject) (new JSONParser()).parse(strResponse);
		    
		    if(DEBUG){
		    	//System.out.println("DEBUG GoogleMapsProxyResource: the http response is " + strResponse);
		    	System.out.println("DEBUG GoogleMapsProxyResource: the string response is " + strResponse);
		    	//System.out.println("DEBUG GoogleMapsProxyResource: the json response is " + jsonResponse);
		    }
	 	
		    // Process the response if the status is 
		    String status = (String)jsonResponse.get(STATUS);
		    System.out.println("DEBUG GoogleMapsProxyResource: the status is " + status);
		    
		    Map<String,Object> output = new HashMap<String,Object>();
		    
		    
		    // If the status code in the response object from the Google Maps Search Nearby query is OK
		    // process the response and pull out the necessary data
		    if(status.equals(GOOGLE_API_STATUS_OK)){
		    	JSONObject results = (JSONObject) ((JSONArray) jsonResponse.get((Object) RESULTS)).get(0);
			    JSONObject geometry = (JSONObject) results.get((Object) GEOMETRY);
			    JSONObject location = (JSONObject) geometry.get((Object) LOCATION);
			    Double lat = (Double)location.get((Object)LATITUDE);
			    Double lng = (Double)location.get((Object)LONGITUDE);
			    String address = (String) results.get((Object) ADDRESS);
			    String name = (String) results.get((Object) STORE_NAME);

			    //JSONObject opening_hours = (JSONObject) results.get((Object) OPENING_HOURS);
			    //Boolean open_now = (Boolean) opening_hours.get((Object) OPEN_NOW);
			    
			    output.put("lat", lat);
			    output.put("lng", lng);
			    output.put("address", address);
			    output.put("name", name);
			    
			    if(DEBUG){
					System.out.println("DEBUG GoogleMapsProxyResource: location is " + location.toString());
					System.out.println("DEBUG GoogleMapsProxyResource: address is " + address.toString());
					//System.out.println("DEBUG GoogleMapsProxyResource: opening hours is " + opening_hours.toString());
					//System.out.println("DEBUG GoogleMapsProxyResource: open now is " + open_now.toString());
			    }
		    }else{
		    	output.put("status", status );
		    }
			     
		    return output;


		}
	
	
	private static String getLongitude(JSONObject location) {
		String LONGITUDE = "lng";
		return location.get((Object) LONGITUDE).toString();
	}

	private static String getLatitude(JSONObject location) {
		String LATITUDE = "lat";
		return location.get((Object) LATITUDE).toString();
	}

	/**
	 * TODO - this needs to be removed
	 */
	private static URI getURI(String scheme, String host, String path, String address) throws URISyntaxException{
		URI uri = null;
		
		URIBuilder urib = new URIBuilder();
	    urib.setScheme("https");
	    urib.setHost(GOOGLE_MAPS_BASE_URL);
	    urib.setPath(GEOCODE_PATH + RESPONSE_TYPE);
	    urib.addParameter("address", address);
	    
	    uri = urib.build();
	    
	    System.out.println("DEBUG GoogleMapsProxyResource: URI is " + uri);

		return uri;
	}
	
	private static URI buildUriStringFromParamsHash(String scheme, String baseurl, String path, Hashtable<String, String> uriParamsHash, String apikey) throws URISyntaxException {
        URIBuilder urib = new URIBuilder();
        urib.setScheme(scheme); //$NON-NLS-1$
        urib.setHost(baseurl);
        urib.setPath(path);
        urib.addParameter("key", apikey); //$NON-NLS-1$
        
        if (uriParamsHash != null) {
            Set<String> keys = uriParamsHash.keySet();
            for (String key : keys) {
                urib.addParameter(key, uriParamsHash.get(key));
            }
        }
        URI uri = urib.build();
        System.out.println("DEBUG GoogleMapsProxyResource: URI is " + uri);
        
        return uri;
    }
	
	private static HttpResponse httpGet(URI uri) throws ClientProtocolException, IOException{
		
		HttpClient client = HttpClients.createDefault();
		HttpGet request = new HttpGet(uri);
		HttpResponse response = client.execute(request);

		return response;
	}
	
	private static JSONObject getLocation(HttpResponse response) throws org.apache.http.ParseException, IOException, ParseException{
		HttpEntity entity = response.getEntity();
	    String strResponse = EntityUtils.toString(entity);
	    System.out.println(strResponse);
	     
	    String RESULTS = "results";
	    String GEOMETRY = "geometry";
	    String LOCATION = "location";

	    JSONParser parser = new JSONParser(); 
	    System.out.println(parser.getClass().toString());
	    Object p = parser.parse(strResponse);
	    
	    JSONObject jsonResponse =(JSONObject)p;
	    JSONObject results = (JSONObject) ((JSONArray) jsonResponse.get((Object) RESULTS)).get(0);
	    JSONObject geometry = (JSONObject) results.get((Object) GEOMETRY);
	    JSONObject location = (JSONObject) geometry.get((Object) LOCATION);
	    
	    return location;
	}
	
}