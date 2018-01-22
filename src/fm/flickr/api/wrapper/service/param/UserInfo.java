package fm.flickr.api.wrapper.service.param;

import java.io.Serializable;

/**
 * Store infos about a user
 * 
 * @author fmichel
 */
public class UserInfo implements Serializable
{
	private static final long serialVersionUID = -2493393561300158829L;

	public static final String FIELD_SEPARATOR = " #_SEP_# ";

	private String userId;

	private String userName;

	private String location;

	private int photosCount;

	private int numberOfContacts;

	private int numberOfFollowers;

	public UserInfo() {
		super();
		this.userId = null;
		this.userName = null;
		this.location = null;
		this.photosCount = 0;
		this.numberOfContacts = 0;
		this.numberOfFollowers = 0;
	}

	public UserInfo(String userId, String userName, String location, int photosCount, int numberOfContacts, int numberOfFollowers) {
		super();
		this.userId = userId;
		this.userName = userName;
		this.location = location;
		this.photosCount = photosCount;
		this.numberOfContacts = numberOfContacts;
		this.numberOfFollowers = numberOfFollowers;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public int getPhotosCount() {
		return photosCount;
	}

	public void setPhotosCount(int photosCount) {
		this.photosCount = photosCount;
	}

	public String getPhotosCountAsStr() {
		return String.valueOf(photosCount);
	}

	public void setPhotosCount(String photosCount) {
		this.photosCount = Integer.parseInt(photosCount);
	}

	public int getNumberOfContacts() {
		return numberOfContacts;
	}

	public void setNumberOfContacts(int numberOfContacts) {
		this.numberOfContacts = numberOfContacts;
	}

	public int getNumberOfFollowers() {
		return numberOfFollowers;
	}

	public void setNumberOfFollowers(int numberOfFollowers) {
		this.numberOfFollowers = numberOfFollowers;
	}

	public String toString() {
		return userId + " (" + userName + "), " + photosCount + " photos, " + numberOfContacts + " contacts, ";
	}

	public String toFile() {
		return userId + FIELD_SEPARATOR + photosCount + FIELD_SEPARATOR + numberOfContacts;
	}
}
