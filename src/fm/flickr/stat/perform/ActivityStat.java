package fm.flickr.stat.perform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.service.FlickrService;
import fm.flickr.api.wrapper.service.param.PhotoItem;
import fm.flickr.api.wrapper.service.param.PhotoItemInfo;
import fm.flickr.api.wrapper.service.param.PhotoItemsSet;
import fm.flickr.api.wrapper.util.ServiceException;
import fm.util.Config;
import fm.util.Util;

/**
 * Manage the information about the actibity on photos: number of views, comments, notes, favorites, groups, tags.
 * This class provides methods to collect data from Interestingness and save it to csv files, 
 * methods to reload the csv files, but so far no method to process the data
 * (data analysis is performed directly in Excel files)
 *
 * @author fmichel
 *
 */
public class ActivityStat
{
	private static Logger logger = Logger.getLogger(ActivityStat.class.getName());

	private static Configuration config = Config.getConfiguration();

	private static FlickrService service = new FlickrService();

	private static HashMap<Integer, PhotoItemInfo> statistics = new HashMap<Integer, PhotoItemInfo>();

	private static String FIELD_SEPARATOR = ";";

	/**
	 * <p>Retrieve the number favs of photos from Interestingness. The results are saved to a file.</p>
	 * 
	 * @param date date of photos from Interestingness, given in format "YYY-MM-DD"
	 * @param photos photos retrieved from Interestingness
	 * @throws IOException in case the file can't be saved
	 */
	public static void collecAdditionalData(String date, PhotoItemsSet photos) throws IOException {

		HashMap<Integer, PhotoItemInfo> stats = new HashMap<Integer, PhotoItemInfo>();

		int nbPhotosProcessed = 0; // Number of photos explored during the whole process

		// Loop on all photos retrieved at once from Interstingness
		for (PhotoItem photo : photos.getPhotosList()) {
			nbPhotosProcessed++;

			// Read photo infos inluding nb of comments and notes
			PhotoItemInfo info = service.getPhotoInfo(photo.getPhotoId());
			if (info != null) {
				info.setInterestingnessRank(photo.getInterestingnessRank());

				// Read the number of favs
				info.setNbFavs(service.getNbFavs(photo.getPhotoId()));
				stats.put(info.getInterestingnessRank(), info);

				// Read the number of groups
				info.setNbGroups(String.valueOf(service.getPhotoPools(photo.getPhotoId()).size()));
				stats.put(info.getInterestingnessRank(), info);
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

		logger.info("### Processed " + stats.size() + " photos");

		saveActivityFromInterestingPhotos(date, stats, nbPhotosProcessed);
	}

	/**
	 * Save the information collected for a given date into a file named \<date\>.csv
	 * 
	 * @param date
	 * @param stats list of photos information
	 * @param nbPhotosProcessed Number of photos explored during the whole process
	 * @throws IOException
	 */
	private static void saveActivityFromInterestingPhotos(String date, HashMap<Integer, PhotoItemInfo> stats, int nbPhotosProcessed) throws IOException {

		File file = new File(Util.getDir(config.getString("fm.flickr.stat.activity.dir")), date + ".csv");
		FileOutputStream fos = new FileOutputStream(file);
		PrintWriter writer = new PrintWriter(fos);
		logger.info("Saving activity info about " + stats.size() + " photos into file " + file.getCanonicalPath());

		writer.println("# Number of photos processed: " + nbPhotosProcessed);
		writer.println("photo id ; rank ; views ; comments ; favs ; notes; groups; tags; time_after_upload");
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			// Uncomment the line below to set GMT (should be) but it will no longer be compatible with data acquired before
			// sdf.setTimeZone(TimeZone.getTimeZone("GMT"));	// Flickr post time is expressed in GMT
			Date endOfDay = sdf.parse(date + " 23:59:59"); // end of the day being explored
			int timeShift = config.getInt("fm.flickr.stat.time_shift_pst", 9);
			Long endOfDayPST = endOfDay.getTime() + timeShift * 60 * 60 * 1000; // End of day is 23h59 at GMT-8 (PST), and 9 hours later at GMT+1 (CET)

			Collection<PhotoItemInfo> photoItemInfo = stats.values();
			Iterator<PhotoItemInfo> iter = photoItemInfo.iterator();
			while (iter.hasNext()) {
				PhotoItemInfo entry = iter.next();
				writer.print(entry.getPhotoId() + FIELD_SEPARATOR + entry.getInterestingnessRank() + FIELD_SEPARATOR + entry.getNbViews());
				writer.print(FIELD_SEPARATOR + entry.getNbComments() + FIELD_SEPARATOR + entry.getNbFavs() + FIELD_SEPARATOR + entry.getNbNotes());
				writer.print(FIELD_SEPARATOR + entry.getNbGroups() + FIELD_SEPARATOR + entry.getTagsSet().size());

				Date postDate = sdf.parse(entry.getDatePost());
				long diff = endOfDayPST - postDate.getTime();
				long diffHour = diff / 1000 / 3600;
				writer.print(FIELD_SEPARATOR + diffHour);

				writer.println();
			}
		} catch (ParseException e) {
			logger.warn("Invalid date format. Exception: " + e.toString());
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
		String fileName = config.getString("fm.flickr.stat.activity.dir") + date + ".log";
		loadFile(new File(fileName));
		logger.info("### " + statistics.size() + " photos activity loaded.");
	}

	/**
	 * Load the content of the all files for the given month, into the map of statistics
	 * 
	 * @param yearMonth year and month formatted as yyyy-mm
	 */
	public static void loadFilesByMonth(String yearMonth) throws ServiceException {
		// Empty the current data if any
		statistics.clear();

		File dir = new File(config.getString("fm.flickr.stat.activity.dir"));
		if (!dir.exists() || !dir.isDirectory()) {
			String errMsg = "Error: data directory " + config.getString("fm.flickr.stat.activity.dir") + " does not exists.";
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}

		for (File file : dir.listFiles())
			if (file.getName().startsWith(yearMonth))
				loadFile(file);

		logger.info("### " + statistics.size() + " total photos activity loaded for period " + yearMonth);
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

					String[] tokens = str.split(PhotoItemInfo.FIELD_SEPARATOR);
					if (tokens.length != 6)
						logger.warn("Wrong format on line: " + str);
					else {
						PhotoItemInfo inf = new PhotoItemInfo();
						inf.setInterestingnessRank(Integer.valueOf(tokens[0]));
						inf.setPhotoId(tokens[1]);
						inf.setNbViews(tokens[2]);
						inf.setNbComments(tokens[3]);
						inf.setNbFavs(tokens[4]);
						inf.setNbNotes(tokens[5]);

						statistics.put(inf.getInterestingnessRank(), inf);
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
}
