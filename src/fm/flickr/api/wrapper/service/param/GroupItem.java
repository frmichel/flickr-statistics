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

	public GroupItem() {
		groupId = "";
		groupName = "";

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

}
