package fm.flickr.api.wrapper.service.param;

import java.io.Serializable;

/**
 * Additional information on a photo, retrieved by service flickr.photos.getInfo
 * 
 * @author fmichel
 */
public class PhotoItemInfo implements Serializable
{
	private static final long serialVersionUID = 2134290926024295680L;

	public static final String FIELD_SEPARATOR = " #_SEP_# ";

	private String photoId;

	/** The 1-based rank of that photo in Interestingness if it was obtained from Interestingness, otherwise 0 */
	protected int interestingnessRank;

	private String ownerNsid;

	private String ownerUserName;

	private String ownerRealName;

	private String title;

	private String description;

	private String datePost;

	private String dateTake;

	private String pageUrl;

	private String nbViews;

	private String nbNotes;

	private String nbComments;

	private String nbFavs;

	private String nbGroups;

	private TagItemsSet tagsSet;

	// Fields reserved for ActicityStat when reloadnig files acquired previously

	private int nbTags;

	private int timeToExplore;

	private int ownersContacts;
	
	private int ownersPhotos;

	public PhotoItemInfo() {
		this.ownerNsid = "";
		this.interestingnessRank = 0;
		this.ownerUserName = "";
		this.ownerRealName = "";
		this.title = "";
		this.description = "";
		this.datePost = "";
		this.dateTake = "";
		this.pageUrl = "";
		this.nbViews = "";
		this.nbNotes = "";
		this.nbComments = "";
		this.nbFavs = "";
		this.nbGroups = "";
		tagsSet = null;
	}

	public String getOwnerNsid() {
		return ownerNsid;
	}

	public void setOwnerNsid(String owner_nsid) {
		this.ownerNsid = owner_nsid;
	}

	public String getOwnerUserName() {
		return ownerUserName;
	}

	public void setOwnerUserName(String ownerUserName) {
		this.ownerUserName = ownerUserName;
	}

	public String getOwnerRealName() {
		return ownerRealName;
	}

	public void setOwnerRealName(String ownerRealName) {
		this.ownerRealName = ownerRealName;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDatePost() {
		return datePost;
	}

	public void setDatePost(String datePost) {
		this.datePost = datePost;
	}

	public String getDateTake() {
		return dateTake;
	}

	public void setDateTake(String dateTake) {
		this.dateTake = dateTake;
	}

	public String getPageUrl() {
		return pageUrl;
	}

	public void setPageUrl(String pageUrl) {
		this.pageUrl = pageUrl;
	}

	public String getPhotoId() {
		return photoId;
	}

	public void setPhotoId(String photoId) {
		this.photoId = photoId;
	}

	public String getNbNotes() {
		return nbNotes;
	}

	public void setNbNotes(String nbNotes) {
		this.nbNotes = nbNotes;
	}

	public String getNbComments() {
		return nbComments;
	}

	public void setNbComments(String nbComments) {
		this.nbComments = nbComments;
	}

	public String getNbFavs() {
		return nbFavs;
	}

	public void setNbFavs(String nbFavs) {
		this.nbFavs = nbFavs;
	}

	public TagItemsSet getTagsSet() {
		return tagsSet;
	}

	public void setTagsSet(TagItemsSet tagsSet) {
		this.tagsSet = tagsSet;
	}

	public int getInterestingnessRank() {
		return interestingnessRank;
	}

	public void setInterestingnessRank(int interestingnessRank) {
		this.interestingnessRank = interestingnessRank;
	}

	public String getNbViews() {
		return nbViews;
	}

	public void setNbViews(String nbViews) {
		this.nbViews = nbViews;
	}

	public String getNbGroups() {
		return nbGroups;
	}

	public void setNbGroups(String nbGroups) {
		this.nbGroups = nbGroups;
	}

	public int getNbTags() {
		return nbTags;
	}

	public void setNbTags(int nbTags) {
		this.nbTags = nbTags;
	}

	public int getTimeToExplore() {
		return timeToExplore;
	}

	public void setTimeToExplore(int timeToExplore) {
		this.timeToExplore = timeToExplore;
	}

	public int getOwnersContacts() {
		return ownersContacts;
	}

	public void setOwnersContacts(int ownersContacts) {
		this.ownersContacts = ownersContacts;
	}

	public int getOwnersPhotos() {
		return ownersPhotos;
	}

	public void setOwnersPhotos(int ownersPhotos) {
		this.ownersPhotos = ownersPhotos;
	}

	@Override
	public String toString() {
		return "PhotoItemInfo [photoId=" + photoId + ", interestingnessRank=" + interestingnessRank + ", ownerNsid=" + ownerNsid + ", ownerUserName=" + ownerUserName + ", ownerRealName=" + ownerRealName + ", title=" + title + ", description=" + description + ", datePost=" + datePost + ", dateTake=" + dateTake + ", pageUrl=" + pageUrl + ", nbViews=" + nbViews + ", nbNotes=" + nbNotes + ", nbComments=" + nbComments + ", nbFavs=" + nbFavs + ", nbGroups=" + nbGroups + ", tagsSet=" + tagsSet + "]";
	}

}
