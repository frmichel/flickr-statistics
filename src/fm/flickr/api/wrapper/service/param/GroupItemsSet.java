package fm.flickr.api.wrapper.service.param;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Implement a set of groups (pools) that a user has subscribed to, or 
 * that a photo belongs to. In the latter, photoId is set, otherwise it is null.
 * 
 * @author fmichel
 */
public class GroupItemsSet implements Serializable
{
	private static final long serialVersionUID = -3204742792110423613L;

	/** List of groups (pools) associated with a photo */
	private ArrayList<GroupItem> groupsList;

	/** Identifier of the photo */
	private String photoId;

	public GroupItemsSet() {
		groupsList = null;
		photoId = null;
	}

	public GroupItemsSet(ArrayList<GroupItem> groupsList, String photoId) {
		this.groupsList = groupsList;
		this.photoId = photoId;
	}

	public GroupItemsSet(ArrayList<GroupItem> groupsList) {
		this.groupsList = groupsList;
		this.photoId = null;
	}

	public ArrayList<GroupItem> getGroupsList() {
		return groupsList;
	}

	public int size() {
		return groupsList.size();
	}

	public boolean isEmpty() {
		return groupsList.size() == 0;
	}

	public void setGroupsList(ArrayList<GroupItem> groupsList) {
		this.groupsList = groupsList;
	}

	public String getPhotoId() {
		return photoId;
	}

	public void setPhotoId(String photoId) {
		this.photoId = photoId;
	}

	/**
	 * Check if the group given by its id is in the list of groups of this GroupItemSet 
	 * @param groupId
	 * @return true if the group is found in the list
	 */
	public boolean containsGroup(String groupId) {
		for (GroupItem group : groupsList) {
			if (group.getGroupId().equals(groupId))
				return true;
		}
		return false;
	}

}
