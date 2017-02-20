package fm.flickr.api.wrapper.service.param;

import java.io.Serializable;

public class Location implements Serializable
{
	private static final long serialVersionUID = -1759760165290762926L;

	public Location() {
		super();
		longitude= "";
		latitude= "";
		country= "";
	}

	public Location(String longitude, String latitude, String country) {
		super();
		this.longitude = longitude;
		this.latitude = latitude;
		this.country = country;
	}

	protected String longitude;

	protected String latitude;

	protected String country;

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}
	
	public Boolean isSet() {
		return !latitude.equals("") && !longitude.equals("");
	}
}
