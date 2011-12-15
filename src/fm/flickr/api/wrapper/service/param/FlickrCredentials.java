package fm.flickr.api.wrapper.service.param;

import java.io.Serializable;

public class FlickrCredentials implements Serializable
{
	private static final long serialVersionUID = -7803826618531873646L;

	private String frob;

	private String token;

	private String nsid;

	private String userName;

	private String fullName;

	public FlickrCredentials() {
	}

	public String getFrob() {
		return frob;
	}

	public void setFrob(String frob) {
		this.frob = frob;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getNsid() {
		return nsid;
	}

	public void setNsid(String nsdi) {
		this.nsid = nsdi;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String toString() {
		return "Frob=" + getFrob() + ", Token=" + getToken() + ", NSID=" + getNsid() + "Username=" + getUserName() + ", Fullname=" + getFullName();
	}

}
