package fm.flickr.stat;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.service.FlickrService;
import fm.flickr.api.wrapper.service.param.PhotoItem;
import fm.flickr.api.wrapper.service.param.PhotoItemsSet;
import fm.flickr.api.wrapper.service.param.UserInfo;

/**
 * This class tries to find users who have a "high centrality", that is who may be influential in the social network.
 * A "high centrality" user has more people following him/her than people he/she follows (contacts), 
 * e.g. 2 follwers for 1 followed.
 * They also have a minimum number of photos and contacts.
 * 
 * Each of these parameter values is defined in static variables.
 * 
 * The starting list of users is taken from a given list of groups. Then centrality is computed.
 * 
 * @author fmichel
*/
public class FindUsersWithCentrality
{
	private static Logger logger = Logger.getLogger(FindUsersWithCentrality.class.getName());

	/** Min number of photos of users */
	private static final int MIN_PHOTOS = 300;

	/** Min number of people followed (contacts) */
	private static final int MIN_FOLLOWED = 30;

	/** Max number of people followed (contacts) */
	private static final int MAX_FOLLOWED = 1000;

	/** Min ratio number of followers / number of contacts */
	private static final float MIN_RATIO_FOLLOWER_FOLLOWED = (float) 4;

	/** Min number of users to collect from one group */
	private static final int NB_USERS_TO_FIND = 20;

	/** Wrapper for Flickr services */
	private static FlickrService service = new FlickrService();

	// Users collected
	private static List<String> users = new ArrayList<String>();

	public static void main(String[] args) {
		try {
			logger.debug("begin");

			// worldwide street photography
			collectUsersFromGroup("647301@N23");

			// Street Photography Worldwide
			collectUsersFromGroup("1794421@N25");

			// Street Cliche
			collectUsersFromGroup("1161775@N22");

			// Street Photography Blog
			collectUsersFromGroup("1576994@N21");

			logger.debug("end");
		} catch (Exception e) {
			logger.error("Unexpected error. Exception: " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Get "high centrality" usesr from a group.
	 * 
	 * Collected users are stored in static variable "users".
	 * 
	 * @param groupId
	 */
	private static void collectUsersFromGroup(String groupId) {

		logger.info("Collecting users from group " + groupId);
		int page = 1;
		int collectedUsers = 0;
		int alreadyCollectedUsers = 0;
		PhotoItemsSet photos = null;

		do {
			// Retrieve the next 500 pictures of the group
			photos = service.getPhotosFromGroup(groupId, 500, page);
			if (photos != null) {
				for (PhotoItem photo : photos.getPhotosList()) {
					String userId = photo.getUserId();
					if (users.contains(userId))
						alreadyCollectedUsers++;
					else {
						UserInfo userInfo = service.getUserInfo(userId);
						if (userInfo != null)
							// Keep only users with min number of photos, min number of people followed (contacts),
							// and at least X followers for one person followed
							if ((userInfo.getPhotosCount() >= MIN_PHOTOS) && (userInfo.getNumberOfContacts() >= MIN_FOLLOWED) && (userInfo.getNumberOfContacts() <= MAX_FOLLOWED) && (userInfo.getNumberOfFollowers() / userInfo.getNumberOfContacts() >= MIN_RATIO_FOLLOWER_FOLLOWED)) {
								users.add(userId);
								System.out.println("https://www.flickr.com/photos/" + userInfo.getUserId());
								collectedUsers++;
							}
					}
				}
			}
			page++;
		} while (page <= photos.getMaxPage() && collectedUsers <= NB_USERS_TO_FIND);

		logger.info("Group " + groupId + ": collected " + collectedUsers + " users, " + alreadyCollectedUsers + " cache hits");
	}
}
