package fm.flickr.api.wrapper.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import fm.flickr.api.wrapper.service.param.FlickrCredentials;
import fm.flickr.api.wrapper.service.param.GroupItem;
import fm.flickr.api.wrapper.service.param.GroupItemsSet;
import fm.flickr.api.wrapper.service.param.PhotoItem;
import fm.flickr.api.wrapper.service.param.PhotoItemInfo;
import fm.flickr.api.wrapper.service.param.PhotoItemsSet;
import fm.flickr.api.wrapper.service.param.TagItem;
import fm.flickr.api.wrapper.service.param.TagItemsSet;
import fm.flickr.api.wrapper.service.param.UserInfo;
import fm.flickr.api.wrapper.util.ServiceException;
import fm.util.Config;

/**
 * This class implements the requests to the Flickr API
 * @author fmichel
 */
public class FlickrService
{
	private static Logger logger = Logger.getLogger(FlickrService.class.getName());

	private static Configuration config = Config.getConfiguration();

	private static enum ImageType {
		SQUARE, THUMBNAIL, MEDIUM, BIG
	};

	private final static String FLICKR_AUTH_URL = "http://flickr.com/services/auth/?";
	private final static String FLICKR_SERVICES_URL = "http://api.flickr.com/services/rest/?";

	public FlickrService() {
		System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
	}

	/**
	 * Calculate the authentication URL the client must click to connect to his account through the
	 * Flickr page
	 */
	public String getConnectUrl() {
		logger.debug("begin getConnectUrl");

		TreeMap<String, String> listParams = new TreeMap<String, String>();
		listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
		listParams.put("perms", "delete");

		String result = FLICKR_AUTH_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);
		logger.debug("Get Connect url service returns: " + result);

		return result;
	}

	/**
	 * Complete the authentication process by asking for a token based on the frob returned by the
	 * Flickr authentication API. The XML response to the getToken query should look like this:
	 * 
	 * <pre>
	 * &lt;auth&gt;
	 * 	&lt;token&gt;67-76598454353455&lt;/token&gt;
	 * 	&lt;perms&gt;write&lt;/perms&gt;
	 * 	&lt;user nsid=&quot;12037949754@N01&quot; username=&quot;Bees&quot; fullname=&quot;Cal H&quot; /&gt;
	 * &lt;/auth&gt;
	 * </pre>
	 * 
	 * @return token, username and fullname if frob found, or null if anything goes wrong
	 */
	public FlickrCredentials getCredentials(String frob) {
		logger.debug("begin getToken, frob=" + frob);
		try {
			// Build the list of parameters
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.auth.getToken");
			listParams.put("frob", frob);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			Document xmlResp = FlickrUtil.launchRequest(urlStr);

			// Get the token node
			Node tokenElt = xmlResp.getElementsByTagName("token").item(0);
			if (tokenElt == null) {
				logger.warn("No token found in the flickr XML response");
				return null;
			}

			// Get the user node
			Element userElt = (Element) xmlResp.getElementsByTagName("user").item(0);
			if (userElt == null) {
				logger.warn("No user found in the flickr XML response");
				return null;
			}

			FlickrCredentials creds = new FlickrCredentials();
			creds.setFrob(frob);
			creds.setToken(tokenElt.getTextContent());
			creds.setNsid(userElt.getAttribute("nsid"));
			creds.setUserName(userElt.getAttribute("username"));
			creds.setFullName(userElt.getAttribute("fullname"));

			logger.info("Returning credentials: " + creds.toString());
			return creds;

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return null;
		}
	}

	/**
	 * Check is a current token is still valid. The XML response to the getToken query should look like this:
	 * 
	 * <pre>
	 * &lt;auth&gt;
	 * 	&lt;token&gt;67-76598454353455&lt;/token&gt;
	 * 	&lt;perms&gt;write&lt;/perms&gt;
	 * 	&lt;user nsid=&quot;12037949754@N01&quot; username=&quot;Bees&quot; fullname=&quot;Cal H&quot; /&gt;
	 * &lt;/auth&gt;
	 * </pre>
	 * 
	 * @return token, username and fullname if token is valid, or null otherwise
	 */
	public FlickrCredentials checkToken(String token) {
		logger.debug("begin checkToken, token=" + token);
		try {
			// Build the list of parameters
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.auth.checkToken");
			listParams.put("auth_token", token);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			Document xmlResp = FlickrUtil.launchRequest(urlStr);

			// Get the user node
			Element userElt = (Element) xmlResp.getElementsByTagName("user").item(0);
			if (userElt == null) {
				logger.warn("No user found in the flickr XML response");
				return null;
			}

			FlickrCredentials creds = new FlickrCredentials();
			creds.setToken(token);
			creds.setNsid(userElt.getAttribute("nsid"));
			creds.setUserName(userElt.getAttribute("username"));
			creds.setFullName(userElt.getAttribute("fullname"));

			logger.info("Returning credentials: " + creds.toString());
			return creds;

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return null;
		}
	}

	/**
	 * Get the last photos of the Flickr Interestingness
	 * 
	 * @param per_page number of images per page
	 * @param page number of the page to get (starts at 1)
	 * @return list of photos, null in case any error occurs
	 */
	public PhotoItemsSet getInterestingnessPhotos(int per_page, int page) {
		return getInterestingnessPhotos(null, per_page, page);
	}

	/**
	 * Get the photos of the Flickr Interestingness at some given date
	 * 
	 * @param date the date of interestingness formatted as YYY-MM-dd. Ignored if null.
	 * @param per_page number of images per page
	 * @param page number of the page to get (starts at 1)
	 * @return list of photos, null in case any error occurs
	 */
	public PhotoItemsSet getInterestingnessPhotos(String date, int per_page, int page) {
		logger.debug("begin getInterestingnessPhotos, per_page=" + per_page + ", page=" + page);

		boolean loop = true;
		while (loop) {
			try {
				TreeMap<String, String> listParams = new TreeMap<String, String>();
				listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
				listParams.put("per_page", Integer.toString(per_page));
				if (date != null)
					listParams.put("date", date);
				listParams.put("page", Integer.toString(page));
				listParams.put("method", "flickr.interestingness.getList");

				// Sign the query and build the url
				String urlStr = FLICKR_SERVICES_URL + FlickrUtil.formatUrlParams(listParams);

				// Call the service and parse the XML response 
				Document xmlResp = FlickrUtil.launchRequest(urlStr);

				// Get the page number and max page
				Element photos = (Element) xmlResp.getElementsByTagName("photos").item(0);
				int maxPages = Integer.valueOf(photos.getAttribute("pages"));
				int pageNumber = Integer.valueOf(photos.getAttribute("page"));

				// Get the list of photo nodes from the response
				NodeList photosList = xmlResp.getElementsByTagName("photo");
				return new PhotoItemsSet(makePhotoListFromNodesList(photosList), pageNumber, maxPages);

			} catch (ServiceException e) {
				logger.error("Error while requesting Flickr service", e);
				if (e.getMessage().contains("Code: 1, Cause: No interesting photos are available for that date")) {
					try {
						logger.warn("Will retry in 5mn");
						Thread.sleep(5*60*1000);
					} catch (InterruptedException ie) {
						logger.warn("Unepected interruption: " + ie.toString());
						e.printStackTrace();
					}
				} else {
					return null;
				}
			}
		}
		// This code should not be accessible
		return null;
	}

	/**
	 * Get the user's favorites photos
	 * 
	 * @param per_page number of images per page
	 * @param page number of the page to get (starts at 1)
	 * @return list of photos, null in case any error occurs
	 */
	public PhotoItemsSet getFavoritePhotos(String token, int per_page, int page) {
		logger.debug("begin getFavoritePhotos, per_page=" + per_page + ", page=" + page);
		try {
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("per_page", Integer.toString(per_page));
			listParams.put("page", Integer.toString(page));
			listParams.put("method", "flickr.favorites.getList");
			listParams.put("auth_token", token);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			Document xmlResp = FlickrUtil.launchRequest(urlStr);

			// Get the page number and max page
			Element photos = (Element) xmlResp.getElementsByTagName("photos").item(0);
			int maxPages = Integer.valueOf(photos.getAttribute("pages"));
			int pageNumber = Integer.valueOf(photos.getAttribute("page"));

			// Get the list of photo nodes from the response
			NodeList photosList = xmlResp.getElementsByTagName("photo");
			return new PhotoItemsSet(makePhotoListFromNodesList(photosList), pageNumber, maxPages);

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return null;
		}
	}

	/**
	 * Get the user's gallery photos
	 * 
	 * @param token the authentication token
	 * @return list of photos, null in case any error occurs
	 */
	public PhotoItemsSet getUserGalleryPhotos(String token, int per_page, int page) {
		logger.debug("begin getUserGalleryPhotos");
		try {
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("user_id", "me");
			listParams.put("per_page", Integer.toString(per_page));
			listParams.put("page", Integer.toString(page));
			listParams.put("method", "flickr.photos.search");
			listParams.put("auth_token", token);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			Document xmlResp = FlickrUtil.launchRequest(urlStr);

			// Get the page number and max page
			Element photos = (Element) xmlResp.getElementsByTagName("photos").item(0);
			int maxPages = Integer.valueOf(photos.getAttribute("pages"));
			int pageNumber = Integer.valueOf(photos.getAttribute("page"));

			// Get the list of photo nodes from the response
			NodeList photosList = xmlResp.getElementsByTagName("photo");
			return new PhotoItemsSet(makePhotoListFromNodesList(photosList), pageNumber, maxPages);

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return null;
		}
	}

	/**
	 * Retrieve extra information about a photo using the getInfo service
	 * 
	 * @param photoId
	 * @return photo infos
	 */
	public PhotoItemInfo getPhotoInfo(String photoId) {
		logger.debug("begin getPhotoInfo, photoId:" + photoId);
		try {
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.photos.getInfo");
			listParams.put("photo_id", photoId);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.formatUrlParams(listParams);

			// Call the service and parse the XML response 
			Document xmlResp = FlickrUtil.launchRequest(urlStr);
			PhotoItemInfo result = new PhotoItemInfo();

			// Set owner data
			Element owner = (Element) xmlResp.getElementsByTagName("owner").item(0);
			result.setOwnerNsid(owner.getAttribute("nsid"));
			result.setOwnerUserName(owner.getAttribute("username"));
			result.setOwnerRealName(owner.getAttribute("realname"));

			// Set title
			Node title = xmlResp.getElementsByTagName("title").item(0);
			result.setTitle(title.getTextContent());

			// Set description
			Node descr = xmlResp.getElementsByTagName("description").item(0);
			result.setDescription(descr.getTextContent());

			// Set dates
			Element dates = (Element) xmlResp.getElementsByTagName("dates").item(0);
			try {
				DateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				// With no change, the current locale is used: CET = GMT+1
				// Uncomment the line below to change it but it will no longer be compatible with data acquired before
				// formater.setTimeZone(TimeZone.getTimeZone("GMT"));	// Flickr post time is expressed in GMT
				String postedS = dates.getAttribute("posted");
				long postedL;

				// VERY WEIRD: seems like the time stamp returned by Flickr is too short
				// by 3 digits! So... adding 3 zeros at the end seems to work fine...
				if (postedS.length() == 10)
					postedL = Long.parseLong(postedS + "000");
				else
					postedL = Long.parseLong(postedS);
				result.setDatePost(formater.format(new Date(postedL)));
			} catch (NumberFormatException e) {
				logger.warn("Unexpected conversion error: " + e.toString());
				result.setDatePost("");
			}
			result.setDateTake(dates.getAttribute("taken"));

			// Set real flickr url and id
			result.setPageUrl(xmlResp.getElementsByTagName("url").item(0).getTextContent());
			result.setPhotoId(photoId);

			// Set number of views
			Element photo = (Element) xmlResp.getElementsByTagName("photo").item(0);
			result.setNbViews(photo.getAttribute("views"));

			// Set number of comments
			Node nbComm = xmlResp.getElementsByTagName("comments").item(0);
			result.setNbComments(nbComm.getTextContent());

			// Set number of notes
			NodeList notes = xmlResp.getElementsByTagName("note");
			result.setNbNotes(String.valueOf(notes.getLength()));

			// Get the list of tags
			NodeList tags = xmlResp.getElementsByTagName("tag");
			result.setTagsSet(new TagItemsSet(makeTagListFromTagNodesList(tags), photoId));

			logger.info("Returning info: " + result.toString());
			return result;
		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return null;
		}
	}

	/**
	 * Retrieve the list of pools (groups) the photo belongs to, from the list of contexts. In the
	 * Flickr API response, pools mean groups, while sets means personal albums.
	 * 
	 * @param photoId
	 * @return list of groups
	 */
	public GroupItemsSet getPhotoPools(String photoId) {
		logger.debug("begin getPhotoPools, photoId:" + photoId);
		try {
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.photos.getAllContexts");
			listParams.put("photo_id", photoId);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			Document xmlResp = FlickrUtil.launchRequest(urlStr);

			// Get the list of pool nodes only from the response
			NodeList pools = xmlResp.getElementsByTagName("pool");
			return new GroupItemsSet(makeGroupListFromContextNodesList(pools), photoId);

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return null;
		}
	}

	/**
	 * Retrieve the list of tags of a photo.
	 * 
	 * @param photoId
	 * @return list of tags
	 */
	public TagItemsSet getPhotoTags(String photoId) {
		logger.debug("begin getPhotoTags, photoId:" + photoId);
		try {
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.tags.getListPhoto");
			listParams.put("photo_id", photoId);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			Document xmlResp = FlickrUtil.launchRequest(urlStr);

			// Get the list of pool nodes only from the response
			NodeList tags = xmlResp.getElementsByTagName("tag");
			return new TagItemsSet(makeTagListFromTagNodesList(tags), photoId);

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return null;
		}
	}

	/**
	 * Retrieve the number of people that have favorited that photo
	 * 
	 * @param photoId
	 * @return number of favorites
	 */
	public String getNbFavs(String photoId) {
		logger.debug("begin getNbFavs, photoId:" + photoId);
		try {
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.photos.getFavorites");
			listParams.put("photo_id", photoId);
			listParams.put("page", "1");

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			Document xmlResp = FlickrUtil.launchRequest(urlStr);

			// Get the number of favorites only
			Element photo = (Element) xmlResp.getElementsByTagName("photo").item(0);
			return photo.getAttribute("total");

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return null;
		}
	}

	/**
	 * Retrieve information about a user
	 * 
	 * @param userId
	 * @return
	 */
	public UserInfo getUserInfo(String userId) {
		logger.debug("begin getUserInfo, userId:" + userId);
		try {
			UserInfo user = new UserInfo();
			user.setUserId(userId);

			//--- Retrieve the user info
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.people.getInfo");
			listParams.put("user_id", userId);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			Document xmlResp = FlickrUtil.launchRequest(urlStr);

			Element element = (Element) xmlResp.getElementsByTagName("username").item(0);
			if (element != null)
				user.setUserName(element.getTextContent());

			element = (Element) xmlResp.getElementsByTagName("location").item(0);
			if (element != null)
				user.setLocation(element.getTextContent());

			element = (Element) xmlResp.getElementsByTagName("count").item(0);
			if (element != null)
				user.setPhotosCount(element.getTextContent());

			//--- Retrieve the list of contacts
			listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.contacts.getPublicList");
			listParams.put("user_id", userId);

			// Sign the query and build the url
			urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			xmlResp = FlickrUtil.launchRequest(urlStr);

			element = (Element) xmlResp.getElementsByTagName("contacts").item(0);
			if (element != null)
				user.setNumberOfContacts(Integer.valueOf(element.getAttribute("total")));

			return user;

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return null;
		}
	}

	/**
	 * Retrieve information about a pool (group)
	 * 
	 * @param groupId
	 * @return list of groups
	 */
	public GroupItem getGroupInfo(String groupId) {
		logger.debug("begin getGroupInfo, groupId:" + groupId);
		try {
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.groups.getInfo");
			listParams.put("group_id", groupId);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			Document xmlResp = FlickrUtil.launchRequest(urlStr);

			GroupItem result = new GroupItem();
			result.setGroupId(groupId);

			// Set group name
			Node title = xmlResp.getElementsByTagName("name").item(0);
			result.setGroupName(title.getTextContent());

			return result;

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return null;
		}
	}

	/**
	 * Retrieve the list of groups user has subscrbied with (for an authenticated user only)
	 * 
	 * @param token the authentication token
	 * @return list of groups
	 */
	public GroupItemsSet getGroupSubscriptions(String token) {
		logger.debug("begin getGroupSubscriptions");
		try {
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.groups.pools.getGroups");
			listParams.put("per_page", "500");
			listParams.put("auth_token", token);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			Document xmlResp = FlickrUtil.launchRequest(urlStr);

			// Get the list of group nodes from the response
			NodeList pools = xmlResp.getElementsByTagName("group");
			return new GroupItemsSet(makeGroupListFromGroupNodesList(pools));

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return null;
		}
	}

	/**
	 * Add a photo to a pool (groups)
	 * 
	 * @param token the authentication token
	 * @param photoId
	 * @param groupItem
	 * @return null if ok, or cause message in case any error occured
	 */
	public String addPhotoToPool(String token, String photoId, GroupItem groupItem) {
		logger.debug("begin addPhotoToPool: photoId=" + photoId + ", group=" + groupItem.toString());
		try {
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.groups.pools.add");
			listParams.put("photo_id", photoId);
			listParams.put("group_id", groupItem.getGroupId());
			listParams.put("auth_token", token);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			FlickrUtil.launchRequest(urlStr);
			return null;

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return e.getMessage();
		}
	}

	/**
	 * Remove a photo from a pool (groups)
	 * 
	 * @param token the authentication token
	 * @param photoId
	 * @param groupItem
	 * @return null if ok, or cause message in case any error occured
	 */
	public String remotePhotoFromPool(String token, String photoId, GroupItem groupItem) {
		logger.debug("begin removePhotoFromPool: photoId=" + photoId + ", group=" + groupItem.toString());
		try {
			TreeMap<String, String> listParams = new TreeMap<String, String>();
			listParams.put("api_key", config.getString("fm.flickr.api.wrapper.flickr_apikey"));
			listParams.put("method", "flickr.groups.pools.remove");
			listParams.put("photo_id", photoId);
			listParams.put("group_id", groupItem.getGroupId());
			listParams.put("auth_token", token);

			// Sign the query and build the url
			String urlStr = FLICKR_SERVICES_URL + FlickrUtil.signApi(config.getString("fm.flickr.api.wrapper.flickr_secret"), listParams);

			// Call the service and parse the XML response 
			FlickrUtil.launchRequest(urlStr);
			return null;

		} catch (ServiceException e) {
			logger.error("Error while requesting Flickr service", e);
			return e.getMessage();
		}
	}

	/**
	 * Build the URL of an image, based on the photo XML element retrieved from the Flickr API (e.g.
	 * interestingness)
	 * 
	 * @param photoElement the photo node from the xml response
	 * @param type the type of the image to return a url for: squ@are (75x75), thumbnail, or medium
	 * @return formatted url according to the scheme:
	 * <code>http://farm{farm-id}.static.flickr.com/{server-id}/id_{secret}....jpg</code>
	 */
	private String getPhotoUrl(Element photoElement, ImageType type) {
		String farmId = photoElement.getAttribute("farm");
		String serverId = photoElement.getAttribute("server");
		String id = photoElement.getAttribute("id");
		String secret = photoElement.getAttribute("secret");

		String url = "http://farm" + farmId + ".static.flickr.com/" + serverId + "/" + id + "_" + secret;
		switch (type) {
			case SQUARE:
				return url + "_s.jpg";
			case THUMBNAIL:
				return url + "_t.jpg";
			case BIG:
				return url + "_b.jpg";
			case MEDIUM:
			default:
				return url + ".jpg";
		}
	}

	/**
	 * Based on an xml list of nodes of type &lt;photo&gt;, build an array list of equivalent
	 * PhotoItem instances.
	 * 
	 * @param photosList list of xml nodes
	 * @return array list of photos, possibly empty.
	 */
	private ArrayList<PhotoItem> makePhotoListFromNodesList(NodeList photosList) {
		ArrayList<PhotoItem> items = new ArrayList<PhotoItem>();

		// For each photo, build a result PhotoItem and poopulate the result list
		for (int i = 0; i < photosList.getLength(); i++) {
			Element photoElement = (Element) photosList.item(i);

			PhotoItem photoItem = new PhotoItem();
			photoItem.setPhotoId(photoElement.getAttribute("id"));
			photoItem.setInterestingnessRank(i + 1);
			photoItem.setSquareUrl(getPhotoUrl(photoElement, ImageType.SQUARE));
			photoItem.setMediumUrl(getPhotoUrl(photoElement, ImageType.MEDIUM));
			photoItem.setBigUrl(getPhotoUrl(photoElement, ImageType.BIG));
			photoItem.setTitle(photoElement.getAttribute("title"));
			photoItem.setUserId(photoElement.getAttribute("owner"));
			items.add(photoItem);
		}

		return items;
	}

	/**
	 * Based on an xml list of contexts (pools and sets a photo belongs to), build an array list of
	 * equivalent GroupItem instances.
	 * 
	 * @param groupsList list of xml nodes
	 * @return array list of groups, possibly empty.
	 */
	private ArrayList<GroupItem> makeGroupListFromContextNodesList(NodeList groupsList) {
		ArrayList<GroupItem> items = new ArrayList<GroupItem>();

		// For each group, build a result GroupItem and populate the result list
		for (int i = 0; i < groupsList.getLength(); i++) {
			Element element = (Element) groupsList.item(i);
			GroupItem item = new GroupItem();

			item.setGroupId(element.getAttribute("id"));
			item.setGroupName(element.getAttribute("title"));
			items.add(item);
		}

		return items;
	}

	/**
	 * Based on an xml list of groups (from flickr.groups.pools.getGroups service), build an array
	 * list of equivalent GroupItem instances.
	 * 
	 * @param groupsList list of xml nodes
	 * @return array list of groups, possibly empty.
	 */
	private ArrayList<GroupItem> makeGroupListFromGroupNodesList(NodeList groupsList) {
		ArrayList<GroupItem> items = new ArrayList<GroupItem>();

		// For each group, build a result GroupItem and populate the result list
		for (int i = 0; i < groupsList.getLength(); i++) {
			Element element = (Element) groupsList.item(i);
			GroupItem item = new GroupItem();

			item.setGroupId(element.getAttribute("id"));
			item.setGroupName(element.getAttribute("name"));
			items.add(item);
		}

		return items;
	}

	/**
	 * Based on an xml list of tags (from flickr.tags.getListPhoto service), build an array
	 * list of equivalent TagItem instances.
	 * 
	 * @param tagList list of xml nodes
	 * @return array list of tags, possibly empty.
	 */
	private ArrayList<TagItem> makeTagListFromTagNodesList(NodeList tagsList) {
		ArrayList<TagItem> items = new ArrayList<TagItem>();

		// For each tag, build a result TagItem and populate the result list
		for (int i = 0; i < tagsList.getLength(); i++) {
			Element element = (Element) tagsList.item(i);
			TagItem item = new TagItem();

			item.setTagId(element.getTextContent());
			item.setRaw(element.getAttribute("raw"));
			items.add(item);
		}
		return items;
	}
}
