package fm.flickr.stat.param;

import fm.flickr.api.wrapper.service.param.TagItem;

/**
 * Store info of a tag in addition to a counter of occurences of photos with that tag,
 * which is used when computing statistics on tags.
 * 
 * @author fmichel
 */
public class TagItemStat extends TagItem implements Comparable<TagItemStat>
{
	private static final long serialVersionUID = -5405070979809913495L;

	public static final String FIELD_SEPARATOR = " #_SEP_# ";

	private int nbOccurences;

	public TagItemStat() {
		super();
		nbOccurences = 0;
	}

	public TagItemStat(String tagId, String raw, int nbOccurences) {
		this.tagId = tagId;
		this.raw = raw;
		this.nbOccurences = nbOccurences;
	}

	public TagItemStat(TagItem tagItem, int nbOccurences) {
		this.tagId = tagItem.getTagId();
		this.raw = tagItem.getRaw();
		this.nbOccurences = nbOccurences;
	}

	public TagItemStat(TagItem tagItem) {
		this.tagId = tagItem.getTagId();
		this.raw = tagItem.getRaw();
		this.nbOccurences = 0;
	}

	public int getNbOccurences() {
		return nbOccurences;
	}

	public void setNbOccurences(int nbOccurences) {
		this.nbOccurences = nbOccurences;
	}

	public void incNbOccurences() {
		this.nbOccurences++;
	}

	public void incNbOccurences(int inc) {
		this.nbOccurences += inc;
	}

	public String toString() {
		return tagId + ": " + nbOccurences + " occurences (" + raw + "): ";
	}

	public String toStringL() {
		return raw + " (" + nbOccurences + " occurences)";
	}

	public String toFile() {
		return tagId + FIELD_SEPARATOR + raw + FIELD_SEPARATOR + nbOccurences;
	}

	@Override
	public int compareTo(TagItemStat tag) {

		if (this.nbOccurences == tag.nbOccurences)
			return 0;
		if (this.nbOccurences < tag.nbOccurences)
			return 1;
		else
			return -1;
	}
}
