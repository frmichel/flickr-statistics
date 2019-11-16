package fm.flickr.stat.perform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.service.FlickrService;
import fm.flickr.api.wrapper.service.param.GroupItemsSet;
import fm.flickr.api.wrapper.service.param.Location;
import fm.flickr.api.wrapper.service.param.PhotoItem;
import fm.flickr.api.wrapper.service.param.PhotoItemInfo;
import fm.flickr.api.wrapper.service.param.PhotoItemsSet;
import fm.flickr.api.wrapper.service.param.UserInfo;
import fm.flickr.api.wrapper.util.ServiceException;
import fm.util.Config;

/**
 * Manage the information about the activity on photos: number of views, comments, notes, favorites,
 * groups, tags, post date and time, ownver's number of photos and contacts.
 * 
 * This class provides methods to collect data about photos, and save data to csv files.
 * Additional methods reload csv files and compute statistics:
 * compute the distribution of photos by nb of views, comments, tags etc.
 *
 * @author fmichel
 *
 */
public class ActivityStat
{
	private static Logger logger = Logger.getLogger(ActivityStat.class.getName());

	private static Configuration config = Config.getConfiguration();

	private static FlickrService service = new FlickrService();

	private static String FIELD_SEPARATOR = ";";

	/** Vector in which all data files are loaded for processing */
	private Vector<PhotoItemInfo> statistics = new Vector<PhotoItemInfo>();

	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	/**
	 * <p>Retrieve detailed information for each photo passed in parameter, as well
	 * as information about the photos'onwers. The results are saved to the file denoted by outputFile.</p>
	 * 
	 * @param outputFile file where to write the data collected
	 * @param date date of photos read, given in format "YYY-MM-DD"
	 * @param photos photos retrieved from Interestingness
	 * @throws IOException in case the file can't be saved
	 */
	public static void collecAdditionalData(File outputFile, String date, PhotoItemsSet photos) throws IOException {

		HashMap<String, PhotoItemInfo> collectedPhoto = new HashMap<String, PhotoItemInfo>();
		HashMap<String, UserInfo> collectedUser = new HashMap<String, UserInfo>();
		int nbPhotosProcessed = 0; // Number of photos collected during the whole process

		// Loop on all photos
		for (PhotoItem photo : photos.getPhotosList()) {

			// Read photo infos inluding nb of comments and notes
			logger.trace("Getting info for photo " + photo.getPhotoId());
			PhotoItemInfo photoInfo = service.getPhotoInfo(photo.getPhotoId());
			if (photoInfo != null) {
				if (collectedPhoto.containsKey(photo.getPhotoId()))
					// This case is unexpected when the photos are taken from Interestingess, but it may occur
					// when their ids are read from a file.
					logger.warn("######## Photo " + photo.getPhotoId() + " has already been treated. Skipping it.");
				else {
					nbPhotosProcessed++;
					photoInfo.setInterestingnessRank(photo.getInterestingnessRank());
					String nbFavs = service.getNbFavs(photo.getPhotoId()); // Read the number of favs
					if (nbFavs != null)
						photoInfo.setNbFavs(nbFavs);
					else
						photoInfo.setNbFavs("0");

					GroupItemsSet grpSet = service.getPhotoPools(photo.getPhotoId()); // Read the number of groups
					if (grpSet != null)
						photoInfo.setNbGroups(String.valueOf(grpSet.size()));
					else
						photoInfo.setNbGroups("0");

					// Read info about the owner of the photo
					logger.trace("Getting info for user " + photoInfo.getOwnerNsid());
					UserInfo userInfo = service.getUserInfo(photoInfo.getOwnerNsid());
					if (userInfo != null) {
						collectedUser.put(photo.getPhotoId(), userInfo);
						collectedPhoto.put(photo.getPhotoId(), photoInfo);
					}
				}
			}

			// Trace activity every 10 photos
			if (nbPhotosProcessed % 10 == 0)
				logger.info("Processed " + nbPhotosProcessed + " photos.");
			else
				logger.debug("Processed " + nbPhotosProcessed + " photos.");

			// Sleep between each photo... just not to be overloading
			try {
				Thread.sleep(config.getInt("fm.flickr.stat.sleepms"));
			} catch (InterruptedException e) {
				logger.warn("Unepected interruption: " + e.toString());
				e.printStackTrace();
			}
		}

		logger.info("### Processed " + collectedPhoto.size() + " photos");
		savePhotosActivity(outputFile, collectedPhoto, collectedUser);
	}

	/**
	 * Save the information collected for a given date into the CSV file denoted by outputFile
	 * 
	 * @param outputFile file where to write the data collected
	 * @param photos map of photos information. The key is the photo id.
	 * @param users maps of information about photo owners. The key is the photo id.
	 * @throws IOException
	 */
	private static void savePhotosActivity(File outputFile, HashMap<String, PhotoItemInfo> photos, HashMap<String, UserInfo> users) throws IOException {

		FileOutputStream fos = new FileOutputStream(outputFile);
		PrintWriter writer = new PrintWriter(fos);
		logger.info("Saving activity info about " + photos.size() + " photos into file " + outputFile.getCanonicalPath());

		writer.println("# Number of photos processed: " + photos.size());
		writer.println("# photo id ; rank ; views ; comments ; favs ; notes; groups; tags; upload_date_time; owner's photos; onwer's contacts; owner's userid; longitude; latitude; country; take_date_time; is_pro_user");

		Collection<PhotoItemInfo> photoItemInfo = photos.values();
		Iterator<PhotoItemInfo> iter = photoItemInfo.iterator();
		while (iter.hasNext()) {
			PhotoItemInfo entry = iter.next();
			// Photo ID; Rank; Views;
			writer.print(entry.getPhotoId() + FIELD_SEPARATOR + entry.getInterestingnessRank() + FIELD_SEPARATOR + entry.getNbViews());
			// Comments; Favs; Notes;
			writer.print(FIELD_SEPARATOR + entry.getNbComments() + FIELD_SEPARATOR + entry.getNbFavs() + FIELD_SEPARATOR + entry.getNbNotes());
			// Groups; Tags;
			writer.print(FIELD_SEPARATOR + entry.getNbGroups() + FIELD_SEPARATOR + entry.getTagsSet().size());
			// Uploaded date/time
			writer.print(FIELD_SEPARATOR + entry.getDatePost());

			// Owner's nb of photos
			writer.print(FIELD_SEPARATOR + users.get(entry.getPhotoId()).getPhotosCount());
			// Owner's nb of contacts
			writer.print(FIELD_SEPARATOR + users.get(entry.getPhotoId()).getNumberOfContacts());
			// Owner's user id
			writer.print(FIELD_SEPARATOR + users.get(entry.getPhotoId()).getUserId());

			// Location
			writer.print(FIELD_SEPARATOR + entry.getLocation().getLongitude());
			writer.print(FIELD_SEPARATOR + entry.getLocation().getLatitude());
			writer.print(FIELD_SEPARATOR + entry.getLocation().getCountry());

			// Take date/time
			writer.print(FIELD_SEPARATOR + entry.getDateTake());

			// Is user pro
			writer.print(FIELD_SEPARATOR + users.get(entry.getPhotoId()).isPro());

			writer.println();
		}

		writer.close();
		fos.close();
	}

	/**
	 * Load the content of the file for the given date, into the map of statistics
	 * 
	 * @param folder where the data files are located
	 * @param date date of data collected from Interestingness, given in format "YYY-MM-DD"
	 */
	public void loadFileByDay(String date, String folder) throws ServiceException {
		String fileName = folder + File.separator + date + ".csv";
		loadFile(new File(fileName));
		logger.info("### " + statistics.size() + " photos activity loaded.");
	}

	/**
	 * Load the content of the all files for the given month, into the map of statistics
	 * 
	 * @param folder where the data files are located
	 * @param yearMonth year and month formatted as yyyy-mm
	 */
	public void loadFilesByMonth(String yearMonth, String folder) throws ServiceException {
		// Clear the current data if any
		statistics.clear();

		File dir = new File(folder);
		if (!dir.exists() || !dir.isDirectory()) {
			String errMsg = "Error: data directory " + folder + " does not exists.";
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}

		for (File file : dir.listFiles())
			if (file.getName().startsWith(yearMonth))
				loadFile(file);

		logger.info("### " + statistics.size() + " total photos activity loaded for period " + yearMonth);
	}

	/** 
	* Parse the content of the given file and store its content into the map statistics
	* @param file  
	*/
	private void loadFile(File file) throws ServiceException {
		try {
			if (!file.exists()) {
				logger.warn("No file: " + file.getAbsolutePath());
				return;
			}

			FileInputStream fis = new FileInputStream(file);
			BufferedReader buffer = new BufferedReader(new InputStreamReader(fis));
			logger.info("### Loading file " + file.getAbsolutePath());

			String str = buffer.readLine();
			while (str != null) {
				if (!str.trim().startsWith("#") && !str.trim().startsWith("photo")) { // ignore comment lines stating with #

					String[] tokens = str.split(FIELD_SEPARATOR);
					if (tokens.length < 9)
						logger.warn("Wrong format on line: " + str);
					else {
						PhotoItemInfo inf = new PhotoItemInfo();
						inf.setPhotoId(tokens[0]);
						inf.setInterestingnessRank(Integer.valueOf(tokens[1]));
						inf.setNbViews(tokens[2]);
						inf.setNbComments(tokens[3]);
						inf.setNbFavs(tokens[4]);
						inf.setNbNotes(tokens[5]);
						inf.setNbGroups(tokens[6]);
						inf.setNbTags(Integer.valueOf(tokens[7]));

						// tokens[8] stored initially a number of hours, after 2012-12-02 it stores the post date and time...
						inf.setDatePost(tokens[8]);

						// Owner's nb of photos and contacts were not introduced at first, so some files
						// may no contain those fields.
						if (tokens.length > 9) {
							inf.setOwnersPhotos(Integer.valueOf(tokens[9]));
							inf.setOwnersContacts(Integer.valueOf(tokens[10]));
						} else {
							inf.setOwnersPhotos(-1);
							inf.setOwnersContacts(-1);
						}

						// Owner's user id
						if (tokens.length > 11)
							inf.setOwnerNsid(tokens[11]);

						// Location data - introduced in December 2016
						if (tokens.length > 12) {
							String longitude = tokens[12];
							String latitude = tokens[13];
							String country = "";
							if (tokens.length >= 15)
								country = tokens[14];
							Location loc = new Location(longitude, latitude, country);
							inf.setLocation(loc);
						} else
							inf.setLocation(new Location("", "", ""));

						// Date taken - introduced in April 2018
						if (tokens.length > 16)
							inf.setDateTake(tokens[16]);

						statistics.add(inf);
					}
				}
				str = buffer.readLine();
			}
			fis.close();

		} catch (IOException e) {
			String errMsg = "Error when reading file " + file.getName() + ". Exception: " + e.toString();
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}
	}

	/**
	 * Init the header line for the distribution of photos by slice of something (groups, views, favs, etc.)
	 * @param ps where to print the output
	 * @param sliceSize size of one slice in the distribution, for instance 10 will give slices "0 to 9", "10 to 19", etc.  
	 * @param nbSlices number of slices, for instance 2 will give 2 slices: "0 to 9", "10 to 19", and a last one ">= 20"
	 */
	public void initComputeDistrib(PrintStream ps, int sliceSize, int nbSlices) throws FileNotFoundException {
		ps.print("# ; ");
		for (int i = 0; i < nbSlices - 1; i++)
			ps.print(sliceSize * i + " to " + (sliceSize * (i + 1) - 1) + "; ");
		ps.print(sliceSize * (nbSlices - 1) + "+ ; ");
		ps.println();
	}

	/**
	 * Print the distribution of number of photos by number of groups they belong to
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm.
	 * It may also be used to denote another category like "explored photos, "any other photos". Cannot be null.
	 */
	public void computeDistribGroup(PrintStream ps, String month) {
		logger.info("Computing distribution of photos by number of groups");
		int nbPhotos = statistics.size();
		int sliceSize = config.getInt("fm.flickr.stat.activity.distrib.group.slice");
		int nbSlices = config.getInt("fm.flickr.stat.activity.distrib.group.nbslices");
		logger.debug("sliceSize: " + sliceSize + ", nbSlices: " + nbSlices + ", nbPhotos: " + nbPhotos);

		if (nbPhotos > 0) {
			Vector<Float> dataToDistribute = new Vector<Float>();
			for (PhotoItemInfo inf : statistics)
				dataToDistribute.add(Float.valueOf(inf.getNbGroups()));
			computeDistrib(ps, month, sliceSize, nbSlices, dataToDistribute);
		}
	}

	/**
	 * Print the distribution of number of photos by number of times they have been viewed
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm.
	 * It may also be used to denote another category like "explored photos, "any other photos". Cannot be null.
	 */
	public void computeDistribViews(PrintStream ps, String month) {
		logger.info("Computing distribution of photos by number of views");
		int nbPhotos = statistics.size();
		int sliceSize = config.getInt("fm.flickr.stat.activity.distrib.view.slice");
		int nbSlices = config.getInt("fm.flickr.stat.activity.distrib.view.nbslices");
		logger.debug("sliceSize: " + sliceSize + ", nbSlices: " + nbSlices + ", nbPhotos: " + nbPhotos);

		if (nbPhotos > 0) {
			Vector<Float> dataToDistribute = new Vector<Float>();
			for (PhotoItemInfo inf : statistics)
				dataToDistribute.add(Float.valueOf(inf.getNbViews()));
			computeDistrib(ps, month, sliceSize, nbSlices, dataToDistribute);
		}
	}

	/**
	 * Print the distribution of number of photos by number of comments
	 * 
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm.
	 * It may also be used to denote another category like "explored photos, "any other photos". Cannot be null.
	 */
	public void computeDistribComments(PrintStream ps, String month) {
		logger.info("Computing distribution of photos by number of comments");
		int nbPhotos = statistics.size();
		int sliceSize = config.getInt("fm.flickr.stat.activity.distrib.comment.slice");
		int nbSlices = config.getInt("fm.flickr.stat.activity.distrib.comment.nbslices");
		logger.debug("sliceSize: " + sliceSize + ", nbSlices: " + nbSlices + ", nbPhotos: " + nbPhotos);

		if (nbPhotos > 0) {
			Vector<Float> dataToDistribute = new Vector<Float>();
			for (PhotoItemInfo inf : statistics)
				dataToDistribute.add(Float.valueOf(inf.getNbComments()));
			computeDistrib(ps, month, sliceSize, nbSlices, dataToDistribute);
		}
	}

	/**
	 * Print the distribution of number of photos by number of favs
	 * 
	 * @param ps the stream where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm.
	 * It may also be used to denote another category like "explored photos, "any other photos". Cannot be null.
	 */
	public void computeDistribFavs(PrintStream ps, String month) {
		logger.info("Computing distribution of photos by number of favs");
		int nbPhotos = statistics.size();
		int sliceSize = config.getInt("fm.flickr.stat.activity.distrib.fav.slice");
		int nbSlices = config.getInt("fm.flickr.stat.activity.distrib.fav.nbslices");
		logger.debug("sliceSize: " + sliceSize + ", nbSlices: " + nbSlices + ", nbPhotos: " + nbPhotos);

		if (nbPhotos > 0) {
			Vector<Float> dataToDistribute = new Vector<Float>();
			for (PhotoItemInfo inf : statistics) {
				if (inf.getNbFavs().equals("null")) {
					// workaround for an unexplained bug
					logger.warn("null string in: " + inf.toString());
					inf.setNbFavs("0");
				}
				dataToDistribute.add(Float.valueOf(inf.getNbFavs()));
			}

			computeDistrib(ps, month, sliceSize, nbSlices, dataToDistribute);
		}
	}

	/**
	 * Print the distribution of number of photos by number of tags
	 * 
	 * @param ps the stream where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm.
	 * It may also be used to denote another category like "explored photos, "any other photos". Cannot be null.
	 */
	public void computeDistribTags(PrintStream ps, String month) {
		logger.info("Computing distribution of photos by number of favs");
		int nbPhotos = statistics.size();
		int sliceSize = config.getInt("fm.flickr.stat.activity.distrib.tag.slice");
		int nbSlices = config.getInt("fm.flickr.stat.activity.distrib.tag.nbslices");
		logger.debug("sliceSize: " + sliceSize + ", nbSlices: " + nbSlices + ", nbPhotos: " + nbPhotos);

		if (nbPhotos > 0) {
			Vector<Float> dataToDistribute = new Vector<Float>();
			for (PhotoItemInfo inf : statistics)
				dataToDistribute.add(Float.valueOf(inf.getNbTags()));
			computeDistrib(ps, month, sliceSize, nbSlices, dataToDistribute);
		}
	}

	/**
	 * Print the distribution of number of photos by total number of photos of the owner
	 * 
	 * @param ps the stream where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm.
	 * It may also be used to denote another category like "explored photos, "any other photos". Cannot be null.
	 */
	public void computeDistribOwnersPhotos(PrintStream ps, String month) {
		logger.info("Computing distribution of photos by number of photos of its owner");
		int nbPhotos = statistics.size();
		int sliceSize = config.getInt("fm.flickr.stat.activity.distrib.user_photo.slice");
		int nbSlices = config.getInt("fm.flickr.stat.activity.distrib.user.nbslices");
		logger.debug("sliceSize: " + sliceSize + ", nbSlices: " + nbSlices + ", nbPhotos: " + nbPhotos);

		if (nbPhotos > 0) {
			Vector<Float> dataToDistribute = new Vector<Float>();
			for (PhotoItemInfo inf : statistics)
				if (inf.getOwnersPhotos() != -1)
					dataToDistribute.add(Float.valueOf(inf.getOwnersPhotos()));
			computeDistrib(ps, month, sliceSize, nbSlices, dataToDistribute);
		}
	}

	/**
	 * Print the distribution of number of photos by total number of contacts of the owner
	 * 
	 * @param ps the stream where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm.
	 * It may also be used to denote another category like "explored photos, "any other photos". Cannot be null.
	 */
	public void computeDistribOwnersContacts(PrintStream ps, String month) {
		logger.info("Computing distribution of photos by number of contacts of its owner");
		int nbPhotos = statistics.size();
		int sliceSize = config.getInt("fm.flickr.stat.activity.distrib.user_contact.slice");
		int nbSlices = config.getInt("fm.flickr.stat.activity.distrib.user.nbslices");
		logger.debug("sliceSize: " + sliceSize + ", nbSlices: " + nbSlices + ", nbPhotos: " + nbPhotos);

		if (nbPhotos > 0) {
			Vector<Float> dataToDistribute = new Vector<Float>();
			for (PhotoItemInfo inf : statistics)
				if (inf.getOwnersPhotos() != -1)
					dataToDistribute.add(Float.valueOf(inf.getOwnersContacts()));
			computeDistrib(ps, month, sliceSize, nbSlices, dataToDistribute);
		}
	}

	/**
	 * Init the header line for the distribution of number of photos by whether they are geo-taggued or not 
	 * @param ps where to print the output
	 */
	public void initComputeDistribLocation(PrintStream ps) throws FileNotFoundException {
		ps.println("#; yes; no");
	}

	/**
	 * Print the distribution of photos by wether they have location or not: yes or no.
	 * 
	 * @param ps the stream where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm.
	 * It may also be used to denote another category like "explored photos, "any other photos". Cannot be null.
	 */
	public void computeDistribLocation(PrintStream ps, String month) {
		logger.info("Computing distribution of photos by number of favs");

		int nbPhotos = statistics.size();
		if (nbPhotos > 0) {
			int nbYes = 0;
			int nbNo = 0;

			for (PhotoItemInfo inf : statistics)
				if (inf.getLocation() != null)
					if (inf.getLocation().isSet())
						nbYes++;
					else
						nbNo++;
				else
					nbNo++;

			ps.print(month + "; ");
			ps.printf("%2.4f; ", (float) nbYes / nbPhotos);
			ps.printf("%2.4f; ", (float) nbNo / nbPhotos);
			ps.println();
		}
	}

	/**
	 * Init the header line for the distribution of number of photos by upload time 
	 * @param ps where to print the output
	 */
	public void initComputeDistribPostTime(PrintStream ps) throws FileNotFoundException {
		ps.println("### Number of photos grouped by upload time (0h to 23h)");
		ps.println("#; 00; 01; 02; 03; 04; 05; 06; 07; 08; 09; 10; 11; 12; 13; 14; 15; 16; 17; 18; 19; 20; 21; 22; 23");
	}

	/**
	 * Compute the number of photos by upload hour (0 to 23)
	 * 
	 * @param ps the stream where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm.
	 * It may also be used to denote another category like "explored photos, "any other photos". Cannot be null.
	 */
	public void computeDistribPostTime(PrintStream ps, String month) {
		logger.info("Computing statistincs of post time distribution by hour of day");

		// Build the list of post dates 
		List<Date> statPostTimeDate = new ArrayList<Date>();
		for (PhotoItemInfo inf : statistics) {
			try {
				Date postDate = sdf.parse(inf.getDatePost());
				statPostTimeDate.add(postDate);
			} catch (ParseException e) {
				logger.warn("Invalid date format. Exception: " + e.toString());
			}
		}

		// Calculate the distribution of post times on 24h
		GregorianCalendar cal = new GregorianCalendar();
		Vector<Integer> distribution = new Vector<Integer>();

		// Init the distribution
		for (int i = 0; i < 24; i++)
			distribution.add(0);

		for (Date date : statPostTimeDate) {
			cal.setTime(date);
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			distribution.set(hour, distribution.get(hour) + 1);
		}

		// Print the results cut down by hour of day, from 0h to 23h
		ps.print(month + "; ");
		for (int i = 0; i < 24; i++) {
			ps.print(distribution.get(i));
			if (i < 24)
				ps.print("; ");
		}
		ps.println();
	}

	/**
	 * Init the header line for the distribution of number of photos by upload time 
	 * @param ps where to print the output
	 */
	public void initComputeUserStat(PrintStream ps) throws FileNotFoundException {
		ps.println("### Number of contacts and photos per user");
		ps.println("#; avg contacts/user; std dev contacts/user; max contacts/user; avg photos/user; std dev photos/user; max photos/user");
	}

	/**
	 * Display users'average number of photos and contacts
	 * 
	 * @param ps the stream where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm.
	 * It may also be used to denote another category like "explored photos, "any other photos". Cannot be null.
	 */
	public void computeUserStat(PrintStream ps, String month) {
		logger.info("Computing users' average number of photos and contacts");

		int nbEntries = statistics.size();
		if (nbEntries > 0) {
			int sumPhotos = 0; // Sum of the number of photos of users
			int maxPhotos = 0; // Maximum number of photos of users
			int sumContacts = 0; // sum of the number of contacts of users
			int maxContacts = 0; // max number of contacts of users

			// Build the list of users (the map is used to eliminate duplicates)
			// Note: until 11/2016, the ownder ID was not collected. Therefore duplicates were
			// counted as different users. This creates quite a difference starting at 12/2016.
			HashMap<String, UserInfo> usrMap = new HashMap<String, UserInfo>();
			for (PhotoItemInfo inf : statistics) {
				UserInfo usrInfo = new UserInfo();
				usrInfo.setUserId(inf.getOwnerNsid());
				usrInfo.setPhotosCount(inf.getOwnersPhotos());
				usrInfo.setNumberOfContacts(inf.getOwnersContacts());
				usrMap.put(inf.getOwnerNsid(), usrInfo);
			}

			Collection<UserInfo> usrList = usrMap.values();
			for (UserInfo usrInfo : usrList) {
				int nbPhotos = Integer.valueOf(usrInfo.getPhotosCount());
				sumPhotos += nbPhotos;
				if (nbPhotos > maxPhotos)
					maxPhotos = nbPhotos;

				int nbContacts = Integer.valueOf(usrInfo.getNumberOfContacts());
				sumContacts += nbContacts;
				if (nbContacts > maxContacts)
					maxContacts = nbContacts;
			}

			ps.print(month + "; ");

			// Calculate the  mean absolute difference of number of contacts
			int avg = sumContacts / nbEntries;
			int sumDeviations = 0;
			for (UserInfo inf : usrList)
				sumDeviations += Math.abs(avg - inf.getNumberOfContacts());
			ps.print(sumContacts / nbEntries + "; "); // average
			ps.print(sumDeviations / nbEntries + "; "); //  mean absolute difference of number of contacts
			ps.print(maxContacts + "; "); // Max number of contacts per user

			// Calculate the  mean absolute difference of the number of photos
			avg = sumPhotos / nbEntries;
			sumDeviations = 0;
			for (UserInfo inf : usrList)
				sumDeviations += Math.abs(avg - Integer.valueOf(inf.getPhotosCount()));
			ps.print(sumPhotos / nbEntries + "; "); // Average number of photos per user
			ps.print(sumDeviations / nbEntries + "; "); //  mean absolute difference of number of photos
			ps.println(maxPhotos); // Max number of photos per user
		}
	}

	/**
	 * Print the distribution of number of photos according to the data provided in the vector.
	 * The vector contains a numerical data, typically the number of groups a photo belongs to,
	 * the number of views, comments or favs, etc.
	 * 
	 * @param ps the stream where to print the output
	 * @param month, in case of processing data by month. Formatted as yyyy-mm.
	 * When processing over a period of time, not a month, this may also be denote a category like "explored photos,
	 * "any other photos". Cannot be null.
	 * @param sliceSize size of the slice in the distribution of photos
	 * @param nbSlices number of slices in the distribution of photos
	 * @param dataToDistribute the data to sort in each slice. This is a vector, each entry corresponds to one photo
	 */
	private void computeDistrib(PrintStream ps, String month, int sliceSize, int nbSlices, Vector<Float> dataToDistribute) {

		int nbPhotos = dataToDistribute.size();
		logger.debug("sliceSize: " + sliceSize + ", nbSlices: " + nbSlices + ", nbPhotos: " + nbPhotos);

		if (nbPhotos > 0) {
			// Create the initial distribution array with values 0
			Vector<Integer> distribution = new Vector<Integer>();
			for (int i = 0; i < nbSlices; i++)
				distribution.add(0);

			for (Float data : dataToDistribute) {
				// Calculate the slice in which this data should be counted
				int sliceIndex = new Double(data / sliceSize).intValue();

				// Limit the max number of slices: any data over nbSlices*sliceSize will be in the last catch-all slice
				if (sliceIndex > (nbSlices - 1))
					sliceIndex = nbSlices - 1;
				distribution.set(sliceIndex, distribution.get(sliceIndex) + 1);
			}

			ps.print(month + "; ");
			Iterator<Integer> iter = distribution.iterator();
			while (iter.hasNext())
				ps.printf("%2.4f; ", (float) (iter.next()) / nbPhotos);
			ps.println();
		}
	}

	/**
	 * Calculate the number of photos posted hour by hour over 24h.
	 * 
	 * @return a vector of 24 values: one value by hour of the day
	 */
	public Vector<Integer> getPostTimeDistrib() throws ServiceException {

		GregorianCalendar cal = new GregorianCalendar();
		Vector<Integer> distribution = new Vector<Integer>();

		// Init the distribution
		for (int i = 0; i < 24; i++)
			distribution.add(0);

		try {
			for (PhotoItemInfo item : statistics) {
				Date date = sdf.parse(item.getDatePost());
				cal.setTime(date);
				int hour = cal.get(Calendar.HOUR_OF_DAY);
				distribution.set(hour, distribution.get(hour) + 1);
			}
			return distribution;

		} catch (ParseException e) {
			String errMsg = "Invalid date format. Exception: " + e.toString();
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}
	}
}
