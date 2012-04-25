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

	public GroupItem() {
		groupId = "";
		groupName = "";
		nbPhotos = "";
		nbMembers = "";
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

	public String toString() {
		return groupId + " (" + groupName + ")";
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

}
