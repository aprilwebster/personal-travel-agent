package com.ibm.watson.travelagentapp.webservices.watson;

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



public class WatsonEmotionServiceProxyResource{
	//gateway-a.watsonplatform.net/calls/text/TextGetEmotion?outputMode=json&apikey=b145d4ee295400cba307da9a11c6bde6c4a9866c&text="I'm so frustrated!" 
	private static final String WATSON_BASE_URL = "gateway-a.watsonplatform.net/";
    private static final String GET_EMOTION_PATH = "calls/text/TextGetEmotion";
    private static final String RESPONSE_TYPE = "json";
    private static final String ALCHEMY_API_KEY = "b145d4ee295400cba307da9a11c6bde6c4a9866c";
  
    
	public static void main(String[] args) throws IOException, ParseException, URISyntaxException{
			
			String text = args[1];
			Double emotion = getAnger(text);
			

		
	}
	
	public static Double getAnger(String text) throws ClientProtocolException, IOException, ParseException, URISyntaxException{
		
		Hashtable<String, String> uriParamsHash = new Hashtable<String, String>();
		uriParamsHash.put("text", text);
		uriParamsHash.put("outputMode", "json");
		uriParamsHash.put("apikey", ALCHEMY_API_KEY);

		URI uri = buildUriStringFromParamsHash("https", WATSON_BASE_URL, GET_EMOTION_PATH, uriParamsHash);	
		HttpResponse response = httpGet(uri);

		HttpEntity entity = response.getEntity();
	    String strResponse = EntityUtils.toString(entity);
	    System.out.println(strResponse);
	     
	    String EMOTIONS = "docEmotions";
	    String ANGER = "anger";

	    JSONParser parser=new JSONParser();
	    Object p = parser.parse(strResponse);
	    
	    JSONObject jsonResponse =(JSONObject)p;
	    JSONObject emotions = (JSONObject) jsonResponse.get(EMOTIONS);
	    Double anger = Double.parseDouble((String) emotions.get(ANGER));
	    System.out.println("Emotions are " + emotions.toString());
	    
	    return anger;

	}

	
	private static URI buildUriStringFromParamsHash(String scheme, String baseurl, String path, Hashtable<String, String> uriParamsHash) throws URISyntaxException {
        URIBuilder urib = new URIBuilder();
        urib.setScheme(scheme); //$NON-NLS-1$
        urib.setHost(baseurl);
        urib.setPath(path);
        
        if (uriParamsHash != null) {
            Set<String> keys = uriParamsHash.keySet();
            for (String key : keys) {
                urib.addParameter(key, uriParamsHash.get(key));
            }
        }
        URI uri = urib.build();
        System.out.println("DEBUG WatsonEmotionServiceProxyResource: URI is " + uri);
        
        return uri;
    }
	
	private static HttpResponse httpGet(URI uri) throws ClientProtocolException, IOException{
		
		HttpClient client = HttpClients.createDefault();
		HttpGet request = new HttpGet(uri);
		HttpResponse response = client.execute(request);

		return response;
	}
	
	
}