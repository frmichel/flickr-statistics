package fm.flickr.stat.param;

public class TagsPerPhoto
{
	/** Average number of tags of a photo */
	private int avgTagsPerPhoto;

	/** Maximum number of tags of a photo */
	private int maxTagsPerPhoto;

	/** Standard deviation of the number of tags a photo has */
	private int stdDevTagsPerPhoto;

	public int getAvgTagsPerPhoto() {
		return avgTagsPerPhoto;
	}

	public void setAvgTagsPerPhoto(int avgTagsPerPhoto) {
		this.avgTagsPerPhoto = avgTagsPerPhoto;
	}

	public int getMaxTagsPerPhoto() {
		return maxTagsPerPhoto;
	}

	public void setMaxTagsPerPhoto(int maxTagsPerPhoto) {
		this.maxTagsPerPhoto = maxTagsPerPhoto;
	}

	public int getStdDevTagsPerPhoto() {
		return stdDevTagsPerPhoto;
	}

	public void setStdDevTagsPerPhoto(int stdDevTagsPerPhoto) {
		this.stdDevTagsPerPhoto = stdDevTagsPerPhoto;
	}

	public String toString() {
		return "avg tags/photo: " + avgTagsPerPhoto + "; std dev tags/photo: " + stdDevTagsPerPhoto + "; max tags/photo: " + maxTagsPerPhoto;
	}

	public String toCsvFile() {
		return avgTagsPerPhoto + "; " + stdDevTagsPerPhoto + "; " + maxTagsPerPhoto;
	}
}
