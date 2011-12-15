package fm.flickr.stat.param;

public class GroupsPerPhoto
{
	/** Average number of groups a photo belongs to */
	private int avgGroupsPerPhoto;

	/** Maximum number of groups a photo belongs to */
	private int maxGroupsPerPhoto;

	/** Standard deviation of the number of groups a photo belongs to */
	private int stdDevGroupsPerPhoto;

	public int getAvgGroupsPerPhoto() {
		return avgGroupsPerPhoto;
	}

	public void setAvgGroupsPerPhoto(int avgGroupsPerPhoto) {
		this.avgGroupsPerPhoto = avgGroupsPerPhoto;
	}

	public int getMaxGroupsPerPhoto() {
		return maxGroupsPerPhoto;
	}

	public void setMaxGroupsPerPhoto(int maxGroupsPerPhoto) {
		this.maxGroupsPerPhoto = maxGroupsPerPhoto;
	}

	public int getStdDevGroupsPerPhoto() {
		return stdDevGroupsPerPhoto;
	}

	public void setStdDevGroupsPerPhoto(int stdDevGroupsPerPhoto) {
		this.stdDevGroupsPerPhoto = stdDevGroupsPerPhoto;
	}

	public String toString() {
		return "avg groups/photo: " + avgGroupsPerPhoto + "; std dev groups/photo: " + stdDevGroupsPerPhoto + "; max groups/photo: " + maxGroupsPerPhoto;
	}

	public String toCsvFile() {
		return avgGroupsPerPhoto + "; " + stdDevGroupsPerPhoto + "; " + maxGroupsPerPhoto;
	}

}
