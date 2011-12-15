package fm.flickr.api.wrapper.service.param;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Implement a set of tags of a photo 
 * 
 * @author fmichel
 */
public class TagItemsSet implements Serializable
{
	private static final long serialVersionUID = -3871236912977259923L;

	/** List of tags associated with a photo */
	private ArrayList<TagItem> tagList;

	/** Identifier of the photo */
	private String photoId;

	public TagItemsSet() {
		tagList = null;
		photoId = null;
	}

	public TagItemsSet(ArrayList<TagItem> tagList, String photoId) {
		this.tagList = tagList;
		this.photoId = photoId;
	}

	public TagItemsSet(ArrayList<TagItem> tagList) {
		this.tagList = tagList;
		this.photoId = null;
	}

	public ArrayList<TagItem> getTagsList() {
		return tagList;
	}

	public int size() {
		return tagList.size();
	}

	public boolean isEmpty() {
		return tagList.size() == 0;
	}

	public void setTagsList(ArrayList<TagItem> tagList) {
		this.tagList = tagList;
	}

	public String getPhotoId() {
		return photoId;
	}

	public void setPhotoId(String photoId) {
		this.photoId = photoId;
	}

	/**
	 * Check if the tag given by its id is in the list of tags of this TagItemSet 
	 * @param tagId
	 * @return true if the tag is found in the list
	 */
	public boolean containsTag(String tagId) {
		for (TagItem tag : tagList) {
			if (tag.getTagId().equals(tagId))
				return true;
		}
		return false;
	}

}
