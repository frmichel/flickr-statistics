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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.service.FlickrService;
import fm.flickr.api.wrapper.service.param.PhotoItem;
import fm.flickr.api.wrapper.service.param.PhotoItemsSet;
import fm.flickr.api.wrapper.service.param.UserInfo;
import fm.flickr.api.wrapper.util.ServiceException;
import fm.util.Config;
import fm.util.Util;

/**
 * Collect data about users whose photos are explored: number of photos and contacts they have.
 * 
 * Daily and monthly stats will compute the same figures: average and max number of photos and contacts of the users
 * 
 * @author Atreyu
 */
public class UserStat
{
	private static Logger logger = Logger.getLogger(UserStat.class.getName());

	private static Configuration config = Config.getConfiguration();

	private static FlickrService service = new FlickrService();

	private static HashMap<String, UserInfo> statistics = new HashMap<String, UserInfo>();

	/**
	 * <p>Retrieve the user information of photos from Interestingness. The results are saved to a file.</p>
	 * 
	 * @param date date of photos from Interestingness, given in format "YYY-MM-DD"
	 * @param photos photos retrieved from Interestingness
	 * @throws IOException in case the file can't be saved
	 */
	public static void collecAdditionalData(String date, PhotoItemsSet photos) throws IOException {

		HashMap<String, UserInfo> stats = new HashMap<String, UserInfo>();

		int nbPhotosProcessed = 0; // Number of photos explored during the whole process

		// Loop on all photos retrieved at once from Interstingness
		for (PhotoItem photo : photos.getPhotosList()) {
			nbPhotosProcessed++;

			// Read photo infos
			UserInfo info = service.getUserInfo(photo.getUserId());
			if (info != null) {
				if (stats.get(info.getUserId()) == null)
					stats.put(info.getUserId(), info);
			}

			// Trace activity every 10 photos
			if (nbPhotosProcessed % 10 == 0)
				logger.info("Processed " + nbPhotosProcessed + " photos.");

			// Sleep between each photo... just not to be overloading
			try {
				Thread.sleep(config.getInt("fm.flickr.stat.sleepms"));
			} catch (InterruptedException e) {
				logger.warn("Unepected interruption: " + e.toString());
				e.printStackTrace();
			}
		}

		logger.info("### Processed " + stats.size() + " photos from " + stats.size() + " users.");

		saveTimesFromInterestingPhotos(date, stats, nbPhotosProcessed);
	}

	/**
	 * Save the user information into a file
	 * 
	 * @param date
	 * @param stats list of user infos
	 * @param nbPhotosProcessed Number of photos explored during the whole process
	 * @throws IOException
	 */
	private static void saveTimesFromInterestingPhotos(String date, HashMap<String, UserInfo> stats, int nbPhotosProcessed) throws IOException {

		File file = new File(Util.getDir(config.getString("fm.flickr.stat.user.dir")), date + ".log");
		FileOutputStream fos = new FileOutputStream(file);
		PrintWriter writer = new PrintWriter(fos);
		logger.info("Saving info about " + stats.size() + " users into file " + file.getCanonicalPath());

		writer.println("# Number of photos processed: " + nbPhotosProcessed);
		writer.println("# Number of users: " + stats.size());
		writer.println("#");
		writer.println("# userId SEPARATOR number of photos SEPARATOR number of contacts");
		writer.println("#");
		;
		Collection<UserInfo> userInfo = stats.values();
		Iterator<UserInfo> iter = userInfo.iterator();
		while (iter.hasNext()) {
			UserInfo entry = iter.next();
			writer.println(entry.toFile());
		}
		writer.close();
		fos.close();
	}

	/**
	 * Load the content of the file for the given date, into the map of statistics
	 * 
	 * @param date date of data collected from Interestingness, given in format "YYY-MM-DD"
	 */
	public static void loadFileByDay(String date) throws ServiceException {
		String fileName = config.getString("fm.flickr.stat.user.dir") + date + ".log";
		loadFile(new File(fileName));
		logger.info("### " + statistics.size() + " users loaded.");
	}

	/**
	 * Load the content of the all files for the given month, into the map of statistics
	 * 
	 * @param yearMonth year and month formatted as yyyy-mm
	 */
	public static void loadFilesByMonth(String yearMonth) throws ServiceException {
		// Empty the current data if any
		statistics.clear();

		File dir = new File(config.getString("fm.flickr.stat.user.dir"));
		if (!dir.exists() || !dir.isDirectory()) {
			String errMsg = "Error: data directory " + config.getString("fm.flickr.stat.user.dir") + " does not exists.";
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}

		for (File file : dir.listFiles())
			if (file.getName().startsWith(yearMonth))
				loadFile(file);

		logger.info("### " + statistics.size() + " total users loaded for period " + yearMonth);
	}

	/** 
	* Parse the content of the given file and store its content into the static map statistics
	* @param file  
	*/
	private static void loadFile(File file) throws ServiceException {
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
				if (!str.trim().startsWith("#")) { // ignore comment lines stating with #

					String[] tokens = str.split(UserInfo.FIELD_SEPARATOR);
					if (tokens.length != 3)
						logger.warn("Wrong format on line: " + str);
					else {
						UserInfo inf = new UserInfo();
						inf.setUserId(tokens[0]);
						inf.setPhotosCount(tokens[1]);
						inf.setNumberOfContacts(Integer.valueOf(tokens[2]));
						statistics.put(inf.getUserId(), inf);
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
	 * Display numbers of contacts and photos per user with explored photos 
	 * @param ps where to print the output
	 */
	public static void computeStatistics(PrintStream ps) {
		computeMonthlyAvg(ps, null);
		computeMonthlyDistribPhoto(ps, null);
		computeMonthlyDistribContact(ps, null);
	}

	/**
	 * Display numbers of photos and contacts by user with explored photos  
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. May be null.
	 */
	public static void initComputeMonthlyAvg(PrintStream ps) throws FileNotFoundException {
		ps.print("#month; ");
		ps.println("avg contacts/user; std dev contacts/user; max contacts/user; avg photos/user; std dev photos/user; max photos/user");
	}

	/**
	 * Init the header line for the distribution of users by number of photos or contacts
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. May be null.
	 */
	public static void initComputeMonthlyDistrib(PrintStream ps, int sliceSize, int nbSlices) throws FileNotFoundException {
		ps.print("#month; ");
		for (int i = 0; i < nbSlices - 1; i++)
			ps.print(sliceSize * i + " to " + (sliceSize * (i + 1) - 1) + "; ");
		ps.print(sliceSize * (nbSlices - 1) + "+ ; ");
		ps.println();
	}

	/**
	 * Display numbers of contacts and photos per user
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. May be null.
	 */
	public static void computeMonthlyAvg(PrintStream ps, String month) {
		logger.info("Computing statistincs of users average number of photos and contacts");
		int nbUsers = statistics.size();
		if (nbUsers > 0) {
			int sumPhotos = 0; // Sum of the number of photos of users
			int maxPhotos = 0; // Maximum number of photos of users
			int sumContacts = 0; // sum of the number of contacts of users
			int maxContacts = 0; // max number of contacts of users

			Collection<UserInfo> usrCollection = statistics.values();
			ArrayList<UserInfo> usrList = new ArrayList<UserInfo>(usrCollection);
			for (UserInfo inf : usrList) {

				int nbPhotos = Integer.valueOf(inf.getPhotosCount());
				sumPhotos += nbPhotos;
				if (nbPhotos > maxPhotos)
					maxPhotos = nbPhotos;

				int nbContacts = Integer.valueOf(inf.getNumberOfContacts());
				sumContacts += nbContacts;
				if (nbContacts > maxContacts)
					maxContacts = nbContacts;
			}

			if (month == null) {
				ps.println("### Number of contacts and photos per user");
				ps.println("avg contacts/user; std dev contacts/user; max contacts/user; avg photos/user; std dev photos/user; max photos/user");
			}
			if (month != null)
				ps.print(month + "; ");

			// Calculate the standard deviation of number of contacts
			int avg = sumContacts / nbUsers;
			int sumDeviations = 0;
			for (UserInfo inf : usrList)
				sumDeviations += Math.abs(avg - inf.getNumberOfContacts());
			ps.print(sumContacts / nbUsers + "; "); // average
			ps.print(sumDeviations / nbUsers + "; "); // Standard deviation of number of contacts
			ps.print(maxContacts + "; "); // Max number of contacts per user

			// Calculate the standard deviation of the number of photos
			avg = sumPhotos / nbUsers;
			sumDeviations = 0;
			for (UserInfo inf : usrList)
				sumDeviations += Math.abs(avg - Integer.valueOf(inf.getPhotosCount()));
			ps.print(sumPhotos / nbUsers + "; "); // Average number of photos per user
			ps.print(sumDeviations / nbUsers + "; "); // Standard deviation of number of photos
			ps.println(maxPhotos); // Max number of photos per user
		}
	}

	/**
	 * Display the distribution of users by number of photos
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. May be null.
	 */
	public static void computeMonthlyDistribPhoto(PrintStream ps, String month) {
		logger.info("Computing distribution of users per total number of photos");

		int sliceSize = config.getInt("fm.flickr.stat.user.distrib.photo.slice");
		int nbSlices = config.getInt("fm.flickr.stat.user.distrib.nbslices");
		int nbUsers = statistics.size();
		logger.debug("sliceSize: " + sliceSize + ", nbSlices: " + nbSlices + ", nbUsers: " + nbUsers);

		if (nbUsers > 0) {
			// Create the initial distribution array with values 0
			Vector<Integer> distribution = new Vector<Integer>();
			for (int i = 0; i < nbSlices; i++)
				distribution.add(0);

			Collection<UserInfo> usrCollection = statistics.values();
			ArrayList<UserInfo> usrList = new ArrayList<UserInfo>(usrCollection);
			for (UserInfo inf : usrList) {
				// Calculate the slice which this user should be counted in
				int sliceIndex = new Double(Math.floor(Float.valueOf(inf.getPhotosCount()) / sliceSize)).intValue();

				// Limit the max number of slices: any user with more than nbSlices*slice photos will be in the last slice
				if (sliceIndex > (nbSlices - 1))
					sliceIndex = nbSlices - 1;
				distribution.set(sliceIndex, distribution.get(sliceIndex) + 1);
			}

			if (month == null) {
				ps.println("### Distribution of users per number of photos");
				for (int i = 0; i < nbSlices - 1; i++)
					ps.print(sliceSize * i + " to " + (sliceSize * (i + 1) - 1) + "; ");
				ps.print(sliceSize * (nbSlices - 1) + "+ ; ");
				ps.println();
			} else
				ps.print(month + "; ");

			Iterator<Integer> iter = distribution.iterator();
			while (iter.hasNext())
				ps.printf("%2.4f; ", (float) (iter.next()) / nbUsers);
			ps.println();
		}
	}

	/**
	 * Display the distribution of users by number of contacts
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. May be null.
	 */
	public static void computeMonthlyDistribContact(PrintStream ps, String month) {
		logger.info("Computing distribution of users per total number of contacts");

		int sliceSize = config.getInt("fm.flickr.stat.user.distrib.contact.slice");
		int nbSlices = config.getInt("fm.flickr.stat.user.distrib.nbslices");
		int nbUsers = statistics.size();
		logger.debug("sliceSize: " + sliceSize + ", nbSlices: " + nbSlices + ", nbUsers: " + nbUsers);

		if (nbUsers > 0) {
			// Create the initial distribution array with values 0
			Vector<Integer> distribution = new Vector<Integer>();
			for (int i = 0; i < nbSlices; i++)
				distribution.add(0);

			Collection<UserInfo> usrCollection = statistics.values();
			ArrayList<UserInfo> usrList = new ArrayList<UserInfo>(usrCollection);
			for (UserInfo inf : usrList) {
				// Calculate the slice which this user should be counted in
				int sliceIndex = new Double(Math.floor((float) (inf.getNumberOfContacts()) / sliceSize)).intValue();

				// Limit the max number of slices: any user with more than nbSlices*slice photos will be in the last slice
				if (sliceIndex > (nbSlices - 1))
					sliceIndex = nbSlices - 1;
				distribution.set(sliceIndex, distribution.get(sliceIndex) + 1);
			}

			if (month == null) {
				ps.println("### Distribution of users per number of contacts");
				for (int i = 0; i < nbSlices; i++)
					ps.print(sliceSize * i + " to " + (sliceSize * (i + 1) - 1) + "; ");
				ps.println();
			} else
				ps.print(month + "; ");

			Iterator<Integer> iter = distribution.iterator();
			while (iter.hasNext())
				ps.printf("%2.4f; ", (float) (iter.next()) / nbUsers);
			ps.println();
		}
	}
}
