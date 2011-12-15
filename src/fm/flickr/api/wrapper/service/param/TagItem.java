package fm.flickr.api.wrapper.service.param;

import java.io.Serializable;

/**
 * Store infos of a tags of photos
 * 
 * @author fmichel
 */
public class TagItem implements Serializable
{
	private static final long serialVersionUID = 7473003205113002338L;

	protected String tagId;

	protected String raw;

	public TagItem() {
		super();
	}

	public TagItem(String tagId, String raw) {
		super();
		this.tagId = tagId;
		this.raw = raw;
	}

	public String getTagId() {
		return tagId;
	}

	public void setTagId(String tagId) {
		this.tagId = tagId;
	}

	public String getRaw() {
		return raw;
	}

	public void setRaw(String raw) {
		this.raw = raw;
	}

	public String toString() {
		return tagId + " (" + raw + ")";
	}

}
