package fm.flickr.stat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.util.ServiceException;
import fm.flickr.stat.perform.DailyUploadsStat;
import fm.flickr.stat.perform.GroupStat;
import fm.flickr.stat.perform.TagStat;
import fm.flickr.stat.perform.TimeStat;
import fm.flickr.stat.perform.UserStat;
import fm.util.Config;

/** 
 * This main class runs the specific treatments to provide statistics results over a period
 * of time, consolidated per month. A month uses all data files between the 1rst and the last
 * day of that month, whatever the number of files.
 * @author fmichel
*/

public class ProcessMonthlyStats
{
	private static Logger logger = Logger.getLogger(ProcessMonthlyStats.class.getName());

	private static Configuration config = Config.getConfiguration();

	/** File where to write the results of time distribution over 24 hours of post times */
	private static PrintStream psTimeDistrib;

	/** File where to write the results of the day of week distribution of post dates */
	private static PrintStream psTimeDayOfWeek;

	/** File where to write the "time to explore" results */
	private static PrintStream psTimeT2E;

	/** File where to write the users results */
	private static PrintStream psUser;

	/** File where to write the groups results */
	private static PrintStream psGroup;

	/** File where to write the tags results */
	private static PrintStream psTag;

	/** File where to write the uploads results */
	private static PrintStream psUploads;

	public static void main(String[] args) {
		try {
			SimpleDateFormat ymDateFrmt = new SimpleDateFormat("yyyy-MM");
			logger.debug("begin");

			// Turn start and stop dates into GregorianCalendars 
			String startDate = config.getString("fm.flickr.stat.startdate");
			String[] tokensStart = startDate.split("-");
			GregorianCalendar calStart = new GregorianCalendar(Integer.valueOf(tokensStart[0]), Integer.valueOf(tokensStart[1]) - 1, Integer.valueOf(tokensStart[2]));

			String stopDate = config.getString("fm.flickr.stat.enddate");
			String[] tokensEnd = stopDate.split("-");
			GregorianCalendar calEnd = new GregorianCalendar(Integer.valueOf(tokensEnd[0]), Integer.valueOf(tokensEnd[1]) - 1, Integer.valueOf(tokensEnd[2]));

			// Init result output files
			initComputeByMonth();

			//--- Process all files created per month and per type of statistics
			calStart = new GregorianCalendar(Integer.valueOf(tokensStart[0]), Integer.valueOf(tokensStart[1]) - 1, Integer.valueOf(tokensStart[2]));
			while (calStart.before(calEnd)) {

				// Format month to process as yyyy-mm and load the files of that month
				String month = ymDateFrmt.format(calStart.getTime());
				logger.info("Processing data for month " + month);
				try {
					// Load all data files for the given month 
					loadFileByMonth(month);

					// Comupute and write results into output files
					computeStatistics(month);

				} catch (ServiceException e) {
					logger.warn(e.toString());
				}

				// Increase the date by one month, and start again
				calStart.add(GregorianCalendar.MONTH, 1);
			}

			logger.debug("end");

		} catch (Exception e) {
			logger.error("Unexpected error. Exception: " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Specific per type of statistics initialisations, like set up csv file header lines
	 * 
	 * @param yearMonth year and month formatted as yyyy-mm
	 * @throws FileNotFoundException 
	 */
	private static void initComputeByMonth() throws FileNotFoundException {

		if (config.getString("fm.flickr.stat.action.time").equals("on")) {
			psTimeDistrib = new PrintStream(config.getString("fm.flickr.stat.time.dir") + "/monthly_times_distrib.csv");
			new TimeStat().initComputeMonthlyPostTimeDistrib(psTimeDistrib);

			psTimeDayOfWeek = new PrintStream(config.getString("fm.flickr.stat.time.dir") + "/monthly_times_dayweek.csv");
			new TimeStat().initComputeMonthlyPostDayOfWeek(psTimeDayOfWeek);

			psTimeT2E = new PrintStream(config.getString("fm.flickr.stat.time.dir") + "/monthly_times_t2e.csv");
			new TimeStat().initComputeMonthlyT2E(psTimeT2E);
		}

		if (config.getString("fm.flickr.stat.action.user").equals("on")) {
			psUser = new PrintStream(config.getString("fm.flickr.stat.user.dir") + "/monthly_results.csv");
			new UserStat().initComputeMonthly(psUser);
		}

		if (config.getString("fm.flickr.stat.action.group").equals("on")) {
			psGroup = new PrintStream(config.getString("fm.flickr.stat.group.dir") + "/monthly_results.csv");
			new GroupStat().initComputeMonthly(psGroup);
		}

		if (config.getString("fm.flickr.stat.action.tag").equals("on")) {
			psTag = new PrintStream(config.getString("fm.flickr.stat.tag.dir") + "/monthly_results.csv");
			new TagStat().initComputeMonthly(psTag);
		}

		if (config.getString("fm.flickr.stat.action.uploads").equals("on")) {
			psUploads = new PrintStream(config.getString("fm.flickr.stat.uploads.dir") + "/monthly_results.csv");
			new DailyUploadsStat().initComputeMonthly(psUploads);
		}
}

	/**
	 * Runs the loading of data files on the given moth by the dedicated statistics classes
	 * 
	 * @param yearMonth year and month formatted as yyyy-mm
	 * @throws IOException
	 */
	private static void loadFileByMonth(String yearMonth) throws ServiceException {

		if (config.getString("fm.flickr.stat.action.time").equals("on"))
			new TimeStat().loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.user").equals("on"))
			new UserStat().loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			new GroupStat().loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			new TagStat().loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			new DailyUploadsStat().loadFilesByMonth(yearMonth);
}

	/**
	 * Run the specific statistics processings 
	 * @param ps the output where to print results
	 * @param month a string denoting the current month formatted as yyyy-mm
	 */
	private static void computeStatistics(String month) {

		if (config.getString("fm.flickr.stat.action.time").equals("on")) {
			new TimeStat().computeMonthlyPostTimeDistrib(psTimeDistrib, month);
			new TimeStat().computeMonthlyPostDayOfWeek(psTimeDayOfWeek, month);
			new TimeStat().computeMonthlyT2E(psTimeT2E, month);
		}

		if (config.getString("fm.flickr.stat.action.user").equals("on"))
			new UserStat().computeMonthlyStatistics(psUser, month);

		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			new GroupStat().computeMonthlyStatistics(psGroup, month);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			new TagStat().computeMonthlyStatistics(psTag, month);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			new DailyUploadsStat().computeMonthlyStatistics(psUploads, month);
	}
}
