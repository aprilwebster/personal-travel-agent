package com.ibm.watson.movieapp.utilities;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Similarity {
	private static final Boolean DEBUG = false;
	private static final String STORE_PROFILES_DIR = "/store_profiles/";
	private static final String TWITTERID_TO_STORENAME_FILE = "/store_profiles/twitterid_to_storename_map.txt";
	
	private static Map<String,String> twitteridToStorenameMap;
	
	// TODO: Feb 12, 2016 - do on the weekend!
	// Should have a constructor that reads in the files and creates the appropriates maps and profiles, etc
	
	public Similarity(){
		
	}
	
	
	public void setTwitteridToStorenameMap() throws IOException{
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(TWITTERID_TO_STORENAME_FILE);
		String twitteridToStore  = IOUtils.toString(is, "UTF-8"); 
		List<String> storeList = Arrays.asList(twitteridToStore.split("\n"));

		this.twitteridToStorenameMap = new HashMap<String, String>();
		Iterator<String> storeIterator = storeList.iterator();
		while (storeIterator.hasNext()) {
			List<String> storePair = Arrays.asList(storeIterator.next().split(","));
			String storeTwitterId = storePair.get(0).trim();
			String storeName = storePair.get(1).trim();
			twitteridToStorenameMap.put(storeTwitterId, storeName);
		}
		
		if(DEBUG){
			System.out.println("DEBUG Similarity: twitter id to store name file is:\n" + twitteridToStore);
			System.out.println("DEBUG Similarity: twitter id to store name file in lines are:\n" + storeList);
			System.out.println("DEBUG Similarity: store" + twitteridToStorenameMap.toString());
		}
	}
	
	
	public String getClosestMatchingStore(String inputProfileJson) throws ParseException, IOException{
		
		Map<String,Double> inputBig5Map = getBig5Map(inputProfileJson);
		
        Map<String, Map<String, Double>> storesBig5Map = getStoresBig5Map();
        
        Map<String,Double> similarityScores = new HashMap<String,Double>();
        
        Iterator storesIt = storesBig5Map.entrySet().iterator();
        while (storesIt.hasNext()) {
        	Map.Entry store = (Map.Entry)storesIt.next();
        	String storeName = (String) store.getKey();
        	Map<String,Double> storeBig5Map = (Map<String,Double>)store.getValue();
        	
        	Double storeSimilarity = getEuclideanSimilarity(storeBig5Map,inputBig5Map);
        	similarityScores.put(storeName, storeSimilarity);
        }
		return getStoreName(getKeyWithLargestValue(similarityScores));

	}
	
	
	/**
	 * 
	 * @param unsortedMap
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String getKeyWithLargestValue(Map unsortedMap) {	 
		List list = new LinkedList(unsortedMap.entrySet());
	 
		Collections.sort(list, new Comparator<Object>() {
			public int compare(Object o1, Object o2) {
				return ((Comparable<Object>) ((Map.Entry) (o1)).getValue())
							.compareTo(((Map.Entry) (o2)).getValue());
			}
		});
	 
		String closestMatchingStore = new String();
		Double highestScore = 0.0;
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			if((Double)entry.getValue() > highestScore){
				closestMatchingStore = (String)entry.getKey();
			}
		}
		return closestMatchingStore;
	}
	
	
	/**
	 * 
	 * @param unsortedMap
	 * @return
	 */
	public static Map getSortedMapByValue(Map unsortedMap) {	 
		List list = new LinkedList(unsortedMap.entrySet());
	 
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
							.compareTo(((Map.Entry) (o2)).getValue());
			}
		});
	 
		Map sortedMap = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	
	/**
	 * Given a personality profile json string, parses the string as a JSONObject and pulls out the big 5 traits
	 * and their percentage values.
	 * @param profile
	 * @return
	 * @throws ParseException
	 */
	public static Map<String,Double> getBig5Map(String personalityProfile) throws ParseException{
		
		// Parse the personality profile json into a JSONObject
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject)parser.parse(personalityProfile);
		
		// Extract the big 5 traits JSONArray from the personality profile (json)
	    JSONObject tree = (JSONObject) obj.get("tree");
	    JSONObject children = (JSONObject) ((JSONArray) tree.get("children")).get(0);
	    JSONObject grandchildren = (JSONObject) ((JSONArray) children.get("children")).get(0);
	    JSONArray big5Json = (JSONArray) ((JSONArray) grandchildren.get("children"));
	    
	    if(DEBUG){
	    	System.out.println(personalityProfile);
        	System.out.println(obj);
	    	System.out.println(big5Json.toString());
	    	System.out.println(big5Json.toArray().length);
	    }
	    
	    // Extract the names of the big 5 traits and their respective percentages and put into a Map
	    Map<String,Double> inputBig5Map = new HashMap<String,Double>();
	    
	    int big5JsonSize = big5Json.toArray().length;        
	    for(int i=0; i < big5JsonSize; i++){
	    	JSONObject trait = (JSONObject) big5Json.get(i);
	    	
	    	String traitName = trait.get("name").toString();
	    	Double traitPercentage = Double.parseDouble(trait.get("percentage").toString());
	    	inputBig5Map.put(traitName, traitPercentage);
	    	
	    	if(DEBUG){
	        	System.out.println(trait);
	        	System.out.println(traitName + ", " + inputBig5Map.get(traitName));
	        }
	    }
	    
	    // Return the map of traitName, traitPercentage key-value pairs
		return inputBig5Map;
	}
	
	
	/**
	 * 
	 * @param profile1
	 * @param profile2
	 * @return
	 */
	public static Double getEuclideanSimilarity(Map<String,Double> profile1, Map<String,Double> profile2){
		Iterator it = profile1.entrySet().iterator();
        Double cumulativeDistance = 0.0;
        Double similarityScore = 0.0;
        
        while (it.hasNext()) {
        	Double distance = 0.0;
            Map.Entry pair = (Map.Entry)it.next();
            String trait = (String) pair.getKey();
            Double profile1Value = (Double) profile1.get(trait);
            Double profile2Value = (Double) profile2.get(trait);
            distance = Math.pow(profile1Value - profile2Value, 2);
            cumulativeDistance += distance;
            it.remove(); // avoids a ConcurrentModificationException
            
            if(DEBUG){
	            System.out.println(trait);
	            System.out.println("\tprofile1: " + profile1Value);
	            System.out.println("\tprofile2: " + profile2Value); 
	            System.out.println("\tdistance for the trait is " + distance);
	            System.out.println("\tcumulative distance is " + cumulativeDistance);
            }
        }
        
        similarityScore = 1-(Math.sqrt(cumulativeDistance/5));
        
        if(DEBUG){
        	System.out.println("The score is " + similarityScore);
        }
        
        return similarityScore;
	}
	
	/**
	 * TODO - the twitter id to store name map should be loaded when this class is instantiated (Feb 13, 2016)
	 * @param storeId
	 * @return
	 * @throws IOException 
	 */
	public String getStoreName(String storeId) throws IOException{
		
		InputStream is = this.getClass().getClassLoader().getResourceAsStream(TWITTERID_TO_STORENAME_FILE);
		String twitteridToStore  = IOUtils.toString(is, "UTF-8"); 
		List<String> storeList = Arrays.asList(twitteridToStore.split("\n"));

		Map<String,String> big5Map = new HashMap<String, String>();
		Iterator<String> storeIterator = storeList.iterator();
		while (storeIterator.hasNext()) {
			List<String> storePair = Arrays.asList(storeIterator.next().split(","));
			String storeTwitterId = storePair.get(0).trim();
			String storeName = storePair.get(1).trim();
			big5Map.put(storeTwitterId, storeName);
		}
		
		if(DEBUG){
			System.out.println("DEBUG Similarity: twitter id to store name file is:\n" + twitteridToStore);
			System.out.println("DEBUG Similarity: twitter id to store name file in lines are:\n" + storeList);
			System.out.println("DEBUG Similarity: store" + big5Map.toString());
		}
		
	    return big5Map.get(storeId);
	}
	
	
	/**
	 * Parses the store personality profiles (json) available in the /store_profiles directory, pulls the values for 
	 * the big 5 traits for each store and puts each into a hashmap.  The function returns a map where the keys are 
	 * the twitter ids for the stores, and the respective values the map of the big 5 traits and their values.
	 * @return Map where (key,value) is (store twitter id, map of big 5 traits and values for the store)
	 * @throws ParseException
	 * @throws IOException
	 */
	public Map<String,Map<String,Double>> getStoresBig5Map() throws ParseException, IOException{
		
		Map<String,Map<String,Double>> storesBig5Map = new HashMap<String, Map<String,Double>>();
		
		// Create a File for the directory holding the store personality profiles
		File storeProfileDir = new File(this.getClass().getResource(STORE_PROFILES_DIR).getPath());
		
		// Iterate through the files in the directory; only the store profiles have a json extension
		for(String store : storeProfileDir.list()){
			
			String extension = store.substring(store.lastIndexOf(".") + 1, store.length());
			String twitterId = store.substring(0, store.lastIndexOf('.'));
			
			if(extension.equalsIgnoreCase("json")){
				// Read the store personality profile file into an InputStream object, and convert to a String
				InputStream is = this.getClass().getClassLoader().getResourceAsStream(STORE_PROFILES_DIR + store);
				String profile = null;
				try {
					profile = IOUtils.toString(is, "UTF-8");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// For each store personality profile String, pull out the big 5 Map
				Map<String,Double> big5Map = getBig5Map(profile);
				
				// Put the store's big 5 map into the map of all store big 5 maps to return
				storesBig5Map.put(twitterId, big5Map);
			}
			
		}
		
		if(DEBUG){
			System.out.println("DEBUG Similarity: map for stores is " + storesBig5Map.toString());
			System.out.println("DEBUG Similarity: stores with big5 " + storesBig5Map.keySet().toString());
		}
		
	    return storesBig5Map;
	}
}

