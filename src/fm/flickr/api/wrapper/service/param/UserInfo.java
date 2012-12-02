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

	private String photosCount;

	private int numberOfContacts;

	public UserInfo() {
		super();
		this.userId = null;
		this.userName = null;
		this.location = null;
		this.photosCount = null;
		this.numberOfContacts = 0;
	}

	public UserInfo(String userId, String userName, String location, String photosCount, int numberOfContacts) {
		super();
		this.userId = userId;
		this.userName = userName;
		this.location = location;
		this.photosCount = photosCount;
		this.numberOfContacts = numberOfContacts;
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

	public String getPhotosCount() {
		return photosCount;
	}

	public void setPhotosCount(String photosCount) {
		this.photosCount = photosCount;
	}

	public int getNumberOfContacts() {
		return numberOfContacts;
	}

	public void setNumberOfContacts(int numberOfContacts) {
		this.numberOfContacts = numberOfContacts;
	}

	public String toString() {
		return userId + " (" + userName + "), " + photosCount + " photos, " + numberOfContacts + " contacts, ";
	}

	public String toFile() {
		return userId + FIELD_SEPARATOR + photosCount + FIELD_SEPARATOR + numberOfContacts;
	}
}
