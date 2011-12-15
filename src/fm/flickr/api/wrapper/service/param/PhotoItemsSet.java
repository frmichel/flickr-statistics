package fm.flickr.api.wrapper.service.param;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Implement a set of photo items and stores the page number and max pages that this set corresponds
 * to
 * 
 * @author fmichel
 */
public class PhotoItemsSet implements Serializable
{
	private static final long serialVersionUID = -8831087395219854912L;

	private ArrayList<PhotoItem> photosList;

	private int pageNumber;

	private int maxPage;

	public PhotoItemsSet() {
		photosList = null;
		pageNumber = 0;
		maxPage = 0;
	}

	public PhotoItemsSet(ArrayList<PhotoItem> photosList, int pageNumber, int maxPage) {
		this.photosList = photosList;
		this.pageNumber = pageNumber;
		this.maxPage = maxPage;
	}

	public ArrayList<PhotoItem> getPhotosList() {
		return photosList;
	}

	public void setPhotosList(ArrayList<PhotoItem> photosList) {
		this.photosList = photosList;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public int getMaxPage() {
		return maxPage;
	}

	public void setMaxPage(int maxPage) {
		this.maxPage = maxPage;
	}
	
	public int size() {
		return photosList.size();
	}

}
