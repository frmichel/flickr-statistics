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
import fm.flickr.stat.IStat;
import fm.util.Config;
import fm.util.Util;

public class TimeStat implements IStat
{
	private static Logger logger = Logger.getLogger(TimeStat.class.getName());

	private static Configuration config = Config.getConfiguration();

	private static FlickrService service = new FlickrService();

	/** Date and time of post dates, to calculate the distribution of post times */
	private static List<Date> statPostDate = new ArrayList<Date>();

	/** Difference (in hours) between the post date/time, and the end of the day when the photo was explored */
	private static List<Long> statTime2Explo = new ArrayList<Long>();

	/**
	 * <p>Retrieve the list of post dates & times of photos from Interestingness. The results are saved to a file.</p>
	 * 
	 * @param date date of photos from Interestingness, given in format "YYY-MM-DD"
	 * @param photos photos retrieved from Interestingness
	 * @throws IOException in case the file can't be saved
	 */
	@Override
	public void collecAdditionalData(String date, PhotoItemsSet photos) throws IOException {

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
	@Override
	public void loadFileByDay(String date) throws ServiceException {

		String fileName = config.getString("fm.flickr.stat.time.dir") + date + ".log";
		loadFile(new File(fileName));
		logger.info("### " + statPostDate.size() + " post date/times and " + statTime2Explo.size() + " time-to-explore durations loaded from " + fileName);
	}

	/**
	 * Load the content of the all files for the given month, into the map of statistics
	 * 
	 * @param yearMonth year and month formatted as yyyy-mm
	 */
	@Override
	public void loadFilesByMonth(String yearMonth) throws ServiceException {
		// Empty the current data if any
		statPostDate.clear();
		statTime2Explo.clear();

		File dir = new File(config.getString("fm.flickr.stat.time.dir"));
		if (!dir.exists() || !dir.isDirectory()) {
			String errMsg = "Error: data directory " + config.getString("fm.flickr.stat.time.dir") + " does not exists.";
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}

		for (File file : dir.listFiles())
			if (file.getName().startsWith(yearMonth))
				loadFile(file);

		logger.info("### " + statPostDate.size() + " post date/times and " + statTime2Explo.size() + " time-to-explore durations loaded for period " + yearMonth);
	}

	/** 
	 * Parse the content of the given file and store its content into the static map statistics
	 * @param file  
	 */
	private void loadFile(File file) throws ServiceException {
		if (!file.exists()) {
			logger.warn("No file: " + file.getAbsolutePath());
			return;
		}

		String fileName = file.getName();
		String date = fileName.substring(0, fileName.length() - 4); // remove the ".log" at the end to keep only the date

		try {
			FileInputStream fis = new FileInputStream(file);
			BufferedReader buffer = new BufferedReader(new InputStreamReader(fis));

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			// With no change, the current locale is used: CET = GMT+1
			// Uncomment the line below to change it but it will no longer be compatible with data acquired before
			// sdf.setTimeZone(TimeZone.getTimeZone("GMT"));	// Flickr post time is expressed in GMT
			Date endOfDay = sdf.parse(date + " 23:59:59"); // end of the day being explored
			Long endOfDayPST = endOfDay.getTime() + 9*60*60*1000; // End of day is 23h59 at GMT-8 (PST), and 9 hours later at GMT+1 (CET)

			logger.info("### Loading file " + file.getAbsolutePath());
			String str = buffer.readLine();

			while (str != null) {
				if (!str.trim().startsWith("#")) { // ignore comment lines stating with #

					String[] tokens = str.split(PhotoItemInfo.FIELD_SEPARATOR);
					if (tokens.length != 2)
						logger.warn("Wrong format on line: " + str);
					else {
						Date postDate = sdf.parse(tokens[1]);
						statPostDate.add(postDate);

						// Calculate the time (in hours) between the moment the photo was posted and the end of the explorer day
						// which is 23h59 in California (Yahoo servers) that is GMT-8, i.e. 23h59 at GMT + 8 hours
						long diff = endOfDayPST - postDate.getTime();
						long diffHour = diff / 1000 / 3600;
						
						// If the diff is < 0, it means that the photo was be explored before it was posted!
						// So better forget about this case, as the post date has probably been changed manually.
						if (diffHour >= 0)
							statTime2Explo.add(diffHour);
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
	@Override
	public void computeStatistics(PrintStream ps) {
		ps.println("### Post time distribution by hour of day:");
		ps.println("00; 01; 02; 03; 04; 05; 06; 07; 08; 09; 10; 11; 12; 13; 14; 15; 16; 17; 18; 19; 20; 21; 22; 23");
		computePostTimeDistrib(ps);
		ps.println();

		ps.println("### Post date distribution by day of week:");
		ps.println("monday; tuesday; wednersday; thurday; friday; saturday; sunday");
		computeMonthlyPostDayOfWeek(ps);
		ps.println();

		ps.println("### Time to explore:");
		ps.println("avg time to explore (h); std deviation of time to explore (h); max time to explore (h)");
		computeT2E(ps);
		ps.println();
	}

	public void initComputeMonthlyPostTimeDistrib(PrintStream ps) throws FileNotFoundException {
		ps.println("### Per month distribution of photos post times, broken by hour of day from 0h to 23h");
		ps.print("#month; ");
		ps.println("00; 01; 02; 03; 04; 05; 06; 07; 08; 09; 10; 11; 12; 13; 14; 15; 16; 17; 18; 19; 20; 21; 22; 23");
	}

	public void initComputeMonthlyT2E(PrintStream ps) throws FileNotFoundException {
		ps.println("### Time to explore per month: time needed (in hours) for the photo to get into the explorer after it was posted");
		ps.print("#month; ");
		ps.println("avg time to explore (h); std deviation of time to explore (h); max time to explore (h)");
	}

	public void initComputeMonthlyPostDayOfWeek(PrintStream ps) throws FileNotFoundException {
		ps.println("### Per day of week distribution of photos post dates");
		ps.print("#month; ");
		ps.println("monday; tuesday; wednersday; thurday; friday; saturday; sunday");
	}

	/**
	 * Sort post date/times by daily hour and count number of hits per hour
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. 
	 * Otherwise it is left empty.
	 */
	public void computeMonthlyPostTimeDistrib(PrintStream ps, String month) {
		// Calculate the distribution of post times on 24h
		GregorianCalendar cal = new GregorianCalendar();
		Vector<Integer> distribution = new Vector<Integer>();
		for (int i = 0; i < 24; i++)
			distribution.add(0);
		for (Date date : statPostDate) {
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

	public void computePostTimeDistrib(PrintStream ps) {
		computeMonthlyPostTimeDistrib(ps, null);
	}

	/**
	 * Sort post date by dail of week (monday, tuesday...) and count number of hits per day
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. 
	 * Otherwise it is left empty.
	 */
	public void computeMonthlyPostDayOfWeek(PrintStream ps, String month) {
		// Calculate the distribution of post dates on 7 days
		GregorianCalendar cal = new GregorianCalendar();
		Vector<Integer> distribution = new Vector<Integer>();
		for (int i = 0; i < 8; i++)
			// Calendar.<days> are between one based: 1=SUNDAY to 7=SATURDAY
			distribution.add(0);

		for (Date date : statPostDate) {
			cal.setTime(date);
			int day = cal.get(Calendar.DAY_OF_WEEK);
			distribution.set(day, distribution.get(day) + 1);
		}

		// Print the results cut down by day of week, from Monday to Sunday
		if (month != null)
			ps.print(month + "; ");
		ps.print(distribution.get(Calendar.MONDAY) + "; ");
		ps.print(distribution.get(Calendar.TUESDAY) + "; ");
		ps.print(distribution.get(Calendar.WEDNESDAY) + "; ");
		ps.print(distribution.get(Calendar.THURSDAY) + "; ");
		ps.print(distribution.get(Calendar.FRIDAY) + "; ");
		ps.print(distribution.get(Calendar.SATURDAY) + "; ");
		ps.println(distribution.get(Calendar.SUNDAY));
	}

	public void computeMonthlyPostDayOfWeek(PrintStream ps) {
		computeMonthlyPostDayOfWeek(ps, null);
	}

	/**
	 * Calculate the time to explore, i.e. the time needed (in hours) for the photo to get into the explorer after it was posted
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. 
	 * Otherwise it is left empty.
	 */
	public void computeMonthlyT2E(PrintStream ps, String month) {
		long sumT2E = 0; // sum of all "time to explore" durations
		long sumDeviations = 0; // standard deviation of the "time to explore" 
		long maxT2E = 0; // maximum time to explore

		for (Long t2e : statTime2Explo) {
			if (t2e > maxT2E)
				maxT2E = t2e;
			sumT2E += t2e;
		}

		// Calculate the average time needed (in hours) for the photo to get into the explorer after it was posted
		long avg = 0;
		if (!statTime2Explo.isEmpty())
			avg = sumT2E / statTime2Explo.size();

		// Calculate the max time needed (in hours) for the photo to get into the explorer after it was posted
		for (Long t2e : statTime2Explo)
			sumDeviations += Math.abs(t2e - avg);

		// Calculate the standard deviation of the time needed (in hours) for the photo to get into the explorer after it was posted
		long stdDev = 0;
		if (!statTime2Explo.isEmpty())
			stdDev = sumDeviations / statTime2Explo.size();

		// Print the results in 3 columns: avg time to explore (h); std deviation of time to explore (h); max time to explore (h)
		if (month != null)
			ps.print(month + "; ");
		ps.println(avg + "; " + stdDev + "; " + maxT2E);
	}

	public void computeT2E(PrintStream ps) {
		computeMonthlyT2E(ps, null);
	}

}
