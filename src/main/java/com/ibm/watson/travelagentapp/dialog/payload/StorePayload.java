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

package com.ibm.watson.travelagentapp.dialog.payload;


/**
 * <P>
 * Various attributes of a store
 * <P>
 * This is a payload class which carries all the info associated with a store - data retrieved from Google Maps
 * regarding location, price level, and rating. It is instantiated in {@code GoogleMapsProxyResource} and
 * subsequently passed onto {@code WDSBlueMixProxyResource} for sending across to the client-side for 
 * rendering store information.
 * 
 * @author April Webster
 */

public class StorePayload {
    private String id;
    private String name;
    private String address;
    //private Boolean open_now;
    //private Integer price_level;
    //private Double rating;
    //private String vicinity;
    //private Double latitude;
    //private Double longitude;
    //private String homepageUrl;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	

	
	
	
	/*public Boolean getOpen_now() {
		return open_now;
	}
	public void setOpen_now(Boolean open_now) {
		this.open_now = open_now;
	}
	public Integer getPrice_level() {
		return price_level;
	}
	public void setPrice_level(Integer price_level) {
		this.price_level = price_level;
	}
	public Double getRating() {
		return rating;
	}
	public void setRating(Double rating) {
		this.rating = rating;
	}
	public String getVicinity() {
		return vicinity;
	}
	public void setVicinity(String vicinity) {
		this.vicinity = vicinity;
	}
	public Double getLatitude() {
		return latitude;
	}
	public void setLatitude(Double latitude) {
		this.latitude = latitude;
	}
	public Double getLongitude() {
		return longitude;
	}
	public void setLongitude(Double longitude) {
		this.longitude = longitude;
	}
	public String getHomepageUrl() {
		return homepageUrl;
	}
	public void setHomepageUrl(String homepageUrl) {
		this.homepageUrl = homepageUrl;
	}*/




}
