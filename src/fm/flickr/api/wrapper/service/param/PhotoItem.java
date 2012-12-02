package fm.flickr.api.wrapper.service.param;

import java.io.Serializable;

/**
 * Store urls of different sizes of the photo at Flickr
 * 
 * @author fmichel
 */
public class PhotoItem implements Serializable
{
	private static final long serialVersionUID = 2152007065666579429L;

	/** Flickr identifier of the photo */
	protected String photoId;

	/** The 1-based rank of that photo in Interestingness if it was obtained from Interestingness, otherwise 0=non significant */
	protected int interestingnessRank;

	/** URL to the square thumbnail */
	protected String squareUrl;

	/** URL to the medium size photo */
	protected String mediumUrl;

	/** URL to the big size photo */
	protected String bigUrl;

	/** Title associated with the photo */
	protected String title;

	/** User identifier */
	protected String userId;

	public PhotoItem() {
	}

	public String getPhotoId() {
		return photoId;
	}

	public void setPhotoId(String photoId) {
		this.photoId = photoId;
	}

	public String getSquareUrl() {
		return squareUrl;
	}

	public void setSquareUrl(String squareUrl) {
		this.squareUrl = squareUrl;
	}

	public String getMediumUrl() {
		return mediumUrl;
	}

	public void setMediumUrl(String mediumUrl) {
		this.mediumUrl = mediumUrl;
	}

	public String getBigUrl() {
		return bigUrl;
	}

	public void setBigUrl(String bigUrl) {
		this.bigUrl = bigUrl;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public int getInterestingnessRank() {
		return interestingnessRank;
	}

	public void setInterestingnessRank(int interestingnessRank) {
		this.interestingnessRank = interestingnessRank;
	}
}
