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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.service.FlickrService;
import fm.flickr.api.wrapper.util.ServiceException;
import fm.util.Config;
import fm.util.Util;

/**
 * This class retrives the total number of photos uploaded every day, hour by hour to Flickr. 
 * It consolidates reports over a period or by month.
 * 
 * @author fmichel
 * 
 */
public class DailyUploadsStat
{
	private static Logger logger = Logger.getLogger(DailyUploadsStat.class.getName());

	private static Configuration config = Config.getConfiguration();

	private static FlickrService service = new FlickrService();

	private static final int MAX_ATTEMPTS = 12;

	/**
	 * Number of elements per line in the data file: 24 hours + the daily total = 25
	 */
	private static final int ELTS_PER_LINE = 25;

	/**
	 * Distribution of number of uploads (24 elements), 25th elements gives the daily total
	 */
	private static Vector<Long> distribution = new Vector<Long>();
	static {
		// Init the distribution vector with 0
		for (int i = 0; i < ELTS_PER_LINE; i++)
			distribution.add(i, (long) 0);
	}

	/**
	 * <p>
	 * Retrieve the number of photos uploaded hour by hour on the given date. The results are saved to a file.
	 * </p>
	 * 
	 * @param date date given in format "YYY-MM-DD"
	 * @throws IOException in case the file can't be saved
	 */
	public void collecDailyUploads(String date) throws IOException {

		GregorianCalendar cal = new GregorianCalendar();
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// With no change, the current locale is used: CET = GMT+1
		// Uncomment the line below to change it, but then it will no longer be compatible with data acquired before
		// sdf.setTimeZone(TimeZone.getTimeZone("GMT")); // Flickr post time is expressed in GMT

		try {
			cal.setTime(sdfDate.parse(date));
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.HOUR_OF_DAY, 0);

			// Retreive the number of uploads hour by hour (at local time = CET for me)
			long total = 0;
			for (int i = 0; i < ELTS_PER_LINE - 1; i++) {
				Date minDate = cal.getTime();
				cal.add(GregorianCalendar.HOUR_OF_DAY, 1);
				Date maxDate = cal.getTime();

				long nb = 0;
				int attempts = 0;
				while (attempts < MAX_ATTEMPTS) {
					logger.debug("Getting number of uploads at " + i + " hours");
					nb = service.getTotalUploads(sdfDateTime.format(minDate), sdfDateTime.format(maxDate));
					// For an unknown reason, the API sometimes returns 0 the first call, then a real value...
					// So let's try several times if it happens
					if (nb != 0)
						break;
					else {
						attempts++;
						if (attempts >= MAX_ATTEMPTS) {
							logger.warn("Could not get value other than 0 after " + MAX_ATTEMPTS + " attempts");
							break;
						}
						try { // Sleep a few seconds before retrying (increasing with the number of attempts)
							Thread.sleep(attempts * 2000);
						} catch (InterruptedException e) {
							logger.warn("Unepected interruption: " + e.toString());
							e.printStackTrace();
						}
					}
				}

				distribution.set(i, nb);
				total += nb; // calculate the daily total
			}

			// Add the daily total
			distribution.set(ELTS_PER_LINE - 1, total);

			logger.info("### Processed daily uploads for date " + date);
			saveDailyData(date, distribution);

		} catch (ParseException e) {
			logger.warn("Invalid date format. Exception: " + e.toString());
		}
	}

	/**
	 * Save the user information into a file
	 * 
	 * @param date given in format "YYY-MM-DD"
	 * @param distribution number of uploads by hour on that date
	 * @throws IOException
	 */
	private static void saveDailyData(String date, Vector<Long> distribution) throws IOException {

		File file = new File(Util.getDir(config.getString("fm.flickr.stat.uploads.dir")), date + ".csv");
		FileOutputStream fos = new FileOutputStream(file);
		PrintWriter writer = new PrintWriter(fos);

		writer.println("### number of uploads by hour of day:");
		writer.println("0h; 1h; 2h; 3h; 4h; 5h; 6h; 7h; 8h; 9h; 10h; 11h; 12h; 13h; 14h; 15h; 16h; 17h; 18h; 19h; 20h; 21h; 22h; 23h; total");

		// Save per hour figures, and the daily total
		for (int i = 0; i < ELTS_PER_LINE; i++) {
			writer.print(distribution.get(i));
			if (i < ELTS_PER_LINE - 1)
				writer.print("; ");
		}

		writer.close();
		fos.close();
	}

	/**
	 * Load the content of the file for the given date into the distribution vector by adding values to values existing in the vector. Thus, this method can be called several times for several days and cumulate data.
	 * 
	 * @param date given in format "YYY-MM-DD"
	 */
	public void loadFileByDay(String date) throws ServiceException {
		String fileName = config.getString("fm.flickr.stat.uploads.dir") + date + ".log";
		loadFile(new File(fileName));
	}

	/**
	 * Load and comulate the content of the all files for the given month
	 * 
	 * @param yearMonth year and month formatted as "YYY-MM"
	 */
	public void loadFilesByMonth(String yearMonth) throws ServiceException {

		// Reinit the distribution of uploads by hour (including last element = daily total)
		for (int i = 0; i < ELTS_PER_LINE; i++)
			distribution.set(i, (long) 0);

		File dir = new File(config.getString("fm.flickr.stat.uploads.dir"));
		if (!dir.exists() || !dir.isDirectory()) {
			String errMsg = "Error: data directory " + config.getString("fm.flickr.stat.uploads.dir") + " does not exists.";
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}

		for (File file : dir.listFiles())
			if (file.getName().startsWith(yearMonth))
				loadFile(file);
	}

	/**
	 * Load the content of the given file and store its content into the distribution vector
	 * 
	 * @param file file instance denoting the daily file to load
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
				if (!str.trim().startsWith("#") && !str.trim().startsWith("0h")) { // ignore comment lines stating with #
					String[] tokens = str.split(";");
					if (tokens.length != ELTS_PER_LINE)
						logger.warn("Wrong format on line: " + str);
					else {
						for (int i = 0; i < ELTS_PER_LINE; i++)
							distribution.set(i, distribution.get(i) + Long.valueOf(tokens[i].trim()));
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
	 * Display numbers of contacts and photos per user
	 * 
	 * @param ps where to print the output
	 */
	public void computeStatistics(PrintStream ps) {
		computeMonthlyStatistics(ps, null);
	}

	public void initComputeMonthly(PrintStream ps) throws FileNotFoundException {
		ps.println("### number of uploads by hour of day:");
		ps.println("month; 0h; 1h; 2h; 3h; 4h; 5h; 6h; 7h; 8h; 9h; 10h; 11h; 12h; 13h; 14h; 15h; 16h; 17h; 18h; 19h; 20h; 21h; 22h; 23h; total");
	}

	/**
	 * 
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. May be null.
	 */
	public void computeMonthlyStatistics(PrintStream ps, String month) {

		if (month == null) {
			ps.println("### number of uploads by hour of day:");
			ps.println("0h; 1h; 2h; 3h; 4h; 5h; 6h; 7h; 8h; 9h; 10h; 11h; 12h; 13h; 14h; 15h; 16h; 17h; 18h; 19h; 20h; 21h; 22h; 23h; total");
		}
		if (month != null)
			ps.print(month + "; ");

		for (int i = 0; i < ELTS_PER_LINE; i++) {
			ps.print(distribution.get(i));
			if (i < ELTS_PER_LINE - 1)
				ps.print("; ");
		}
		ps.println();
	}
}
