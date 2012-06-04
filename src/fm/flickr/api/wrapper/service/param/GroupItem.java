package fm.flickr.api.wrapper.service.param;

import java.io.Serializable;

/**
 * Store infos of a group (pool) of photos
 * 
 * @author fmichel
 */
public class GroupItem implements Serializable
{
	private static final long serialVersionUID = -1191637241626471600L;

	protected String groupId;

	protected String groupName;

	protected String nbPhotos;

	protected String nbMembers;
	
	protected Boolean isModerated;

	public GroupItem() {
		groupId = "";
		groupName = "";
		nbPhotos = "";
		nbMembers = "";
		isModerated = false;
	}

	public GroupItem(String groupId, String groupName) {
		this.groupId = groupId;
		this.groupName = groupName;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public String getNbPhotos() {
		return nbPhotos;
	}

	public void setNbPhotos(String nbPhotos) {
		this.nbPhotos = nbPhotos;
	}

	public String getNbMembers() {
		return nbMembers;
	}

	public void setNbMembers(String nbMembers) {
		this.nbMembers = nbMembers;
	}

	public Boolean getIsModerated() {
		return isModerated;
	}

	public void setIsModerated(Boolean isModerated) {
		this.isModerated = isModerated;
	}
	
	@Override
	public String toString() {
		return "GroupItem [groupId=" + groupId + ", groupName=" + groupName + ", nbPhotos=" + nbPhotos + ", nbMembers=" + nbMembers + ", isModerated=" + isModerated + "]";
	}


	
}
