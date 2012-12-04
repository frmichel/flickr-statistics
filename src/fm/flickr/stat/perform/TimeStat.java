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
import fm.flickr.api.wrapper.service.param.PhotoItem;
import fm.flickr.api.wrapper.service.param.PhotoItemInfo;
import fm.flickr.api.wrapper.service.param.PhotoItemsSet;
import fm.flickr.api.wrapper.util.ServiceException;
import fm.util.Config;
import fm.util.Util;

/**
 * Collect the post date and time of explored photos
 * 
 * Daily and monthly stats load result files over a period of a full month, and copute:  
 *  - post time distribution by hour of day,
 *  - post time distribution by week day
 *  - average and maximum time to explore
 * 
 * @author Atreyu
 */
public class TimeStat
{
	private static Logger logger = Logger.getLogger(TimeStat.class.getName());

	private static Configuration config = Config.getConfiguration();

	private static FlickrService service = new FlickrService();

	/** Date and time of post dates, to calculate the distribution of post times */
	private static List<Date> statPostTimeDate = new ArrayList<Date>();

	/**
	 * <p>Retrieve the list of post dates & times of photos from Interestingness. The results are saved to a file.</p>
	 * 
	 * @param date date of photos from Interestingness, given in format "YYY-MM-DD"
	 * @param photos photos retrieved from Interestingness
	 * @throws IOException in case the file can't be saved
	 */
	public static void collecAdditionalData(String date, PhotoItemsSet photos) throws IOException {

		HashMap<String, PhotoItemInfo> stats = new HashMap<String, PhotoItemInfo>();

		// Number of photos explored during the whole process
		int nbPhotosProcessed = 0;

		// Loop on all photos retrieved at once from Interstingness
		for (PhotoItem photo : photos.getPhotosList()) {
			nbPhotosProcessed++;

			// Read photo infos
			PhotoItemInfo info = service.getPhotoInfo(photo.getPhotoId());
			if (info != null) {
				stats.put(info.getPhotoId(), info);
			}

			// Trace activity every 10 photos
			if (nbPhotosProcessed % 10 == 0)
				logger.info("Processed " + nbPhotosProcessed + ".");
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

		logger.info("### Processed " + stats.size() + " photos from ");

		saveTimesFromInterestingPhotos(date, stats, nbPhotosProcessed);
	}

	/**
	 * Save the list of photos with their post dates into a file
	 * 
	 * @param date
	 * @param stats list of photos info
	 * @param nbPhotosProcessed Number of photos explored during the whole process
	 * @throws IOException
	 */
	private static void saveTimesFromInterestingPhotos(String date, HashMap<String, PhotoItemInfo> stats, int nbPhotosProcessed) throws IOException {

		File file = new File(Util.getDir(config.getString("fm.flickr.stat.time.dir")), date + ".log");
		FileOutputStream fos = new FileOutputStream(file);
		PrintWriter writer = new PrintWriter(fos);
		logger.info("Saving post date of " + stats.size() + " photos into file " + file.getCanonicalPath());

		writer.println("# Number of photos processed: " + nbPhotosProcessed);
		writer.println("#");
		writer.println("# photo id SEPARATOR post date ");
		writer.println("#");

		Collection<PhotoItemInfo> grpset = stats.values();
		Iterator<PhotoItemInfo> iter = grpset.iterator();
		while (iter.hasNext()) {
			PhotoItemInfo entry = iter.next();
			writer.println(entry.getPhotoId() + PhotoItemInfo.FIELD_SEPARATOR + entry.getDatePost());
		}
		writer.close();
		fos.close();
	}

	/**
	 * Load the content of the file created by saveTimesFromInterestingPhotos() on the given date, into the map of statistics
	 * 
	 * @param stats the list where to store infos read from the file
	 */
	public static void loadFileByDay(String date) throws ServiceException {
		String fileName = config.getString("fm.flickr.stat.time.dir") + date + ".log";
		loadFile(new File(fileName));
		logger.info("### " + statPostTimeDate.size() + " post date/times loaded from " + fileName);
	}

	/**
	 * Load the content of the all files for the given month, into the map of statistics
	 * 
	 * @param yearMonth year and month formatted as yyyy-mm
	 */
	public static void loadFilesByMonth(String yearMonth) throws ServiceException {
		// Empty the current data if any
		statPostTimeDate.clear();

		File dir = new File(config.getString("fm.flickr.stat.time.dir"));
		if (!dir.exists() || !dir.isDirectory()) {
			String errMsg = "Error: data directory " + config.getString("fm.flickr.stat.time.dir") + " does not exists.";
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}

		for (File file : dir.listFiles())
			if (file.getName().startsWith(yearMonth))
				loadFile(file);

		logger.info("### " + statPostTimeDate.size() + " post date/times loaded for period " + yearMonth);
	}

	/** 
	 * Parse the content of the given file and store its content into the static map statistics
	 * @param file  
	 */
	private static void loadFile(File file) throws ServiceException {
		if (!file.exists()) {
			logger.warn("No file: " + file.getAbsolutePath());
			return;
		}

		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedReader buffer = new BufferedReader(new InputStreamReader(fis));
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			logger.info("### Loading file " + file.getAbsolutePath());
			String str = buffer.readLine();

			while (str != null) {
				if (!str.trim().startsWith("#")) { // ignore comment lines stating with #

					String[] tokens = str.split(PhotoItemInfo.FIELD_SEPARATOR);
					if (tokens.length != 2)
						logger.warn("Wrong format on line: " + str);
					else {
						Date postDate = sdf.parse(tokens[1]);
						statPostTimeDate.add(postDate);
					}
				}
				str = buffer.readLine();
			}
			fis.close();
		} catch (ParseException e) {
			logger.warn("Invalid date format. Exception: " + e.toString());
		} catch (IOException e) {
			String errMsg = "Error when reading file " + file.getName() + ". Exception: " + e.toString();
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}
	}

	/**
	 * Sort post date/times by daily hour and count number of hits per hour
	 * @param ps where to print the output
	 * Otherwise it is left empty.
	 */
	public static void computeStatistics(PrintStream ps) {
		logger.info("Computing statistincs of post time distribution by hour of day");
		ps.println("### Post time distribution by hour of day:");
		ps.println("00; 01; 02; 03; 04; 05; 06; 07; 08; 09; 10; 11; 12; 13; 14; 15; 16; 17; 18; 19; 20; 21; 22; 23");
		computePostTimeDistrib(ps);
		ps.println();
	}

	/**
	 * Print the header line following the csv format
	 * @param ps
	 * @throws FileNotFoundException
	 */
	public static void initComputeMonthlyPostTimeDistrib(PrintStream ps) throws FileNotFoundException {
		ps.println("### Per month distribution of photos post times, broken by hour of day from 0h to 23h");
		ps.print("#month; ");
		ps.println("00; 01; 02; 03; 04; 05; 06; 07; 08; 09; 10; 11; 12; 13; 14; 15; 16; 17; 18; 19; 20; 21; 22; 23");
	}

	public static void reset() {
		statPostTimeDate.clear();
	}

	/**
	 * Sort post date/times by daily hour and count number of hits per hour
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. 
	 * Otherwise it is left empty.
	 */
	public static void computeMonthlyPostTimeDistrib(PrintStream ps, String month) {
		// Calculate the distribution of post times on 24h
		GregorianCalendar cal = new GregorianCalendar();
		Vector<Integer> distribution = new Vector<Integer>();
		for (int i = 0; i < 24; i++)
			distribution.add(0);
		for (Date date : statPostTimeDate) {
			cal.setTime(date);
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			distribution.set(hour, distribution.get(hour) + 1);
		}

		// Print the results cut down by hour of day, from 0h to 23h
		if (month != null)
			ps.print(month + "; ");
		for (int i = 0; i < 24; i++) {
			ps.print(distribution.get(i));
			if (i < 24)
				ps.print("; ");
		}
		ps.println();
	}

	/**
	 * Sort post date/times by daily hour and count number of hits per hour and return the distribution
	 */
	public static Vector<Integer> getPostTimeDistrib() {
		// Calculate the distribution of post times on 24h
		GregorianCalendar cal = new GregorianCalendar();
		Vector<Integer> distribution = new Vector<Integer>();
		for (int i = 0; i < 24; i++)
			distribution.add(0);
		for (Date date : statPostTimeDate) {
			cal.setTime(date);
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			distribution.set(hour, distribution.get(hour) + 1);
		}
		return distribution;
	}

	public static void computePostTimeDistrib(PrintStream ps) {
		computeMonthlyPostTimeDistrib(ps, null);
	}
}
