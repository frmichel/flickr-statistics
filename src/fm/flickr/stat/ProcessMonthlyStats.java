package fm.flickr.stat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.util.ServiceException;
import fm.flickr.stat.perform.ActivityStat;
import fm.flickr.stat.perform.DailyUploadsStat;
import fm.flickr.stat.perform.GroupStat;
import fm.flickr.stat.perform.TagStat;
import fm.flickr.stat.perform.TimeStat;
import fm.flickr.stat.perform.UserStat;
import fm.util.Config;

/** 
 * This main class runs the specific treatments to provide statistics results consolidated per month. 
 * All data files between the 1rst and the last day of each month are treated, whatever the number of files.
 * 
 * Monthly stats are generally stored into csv files, conversely to daily stats that are displayed
 * on the std output. 
 * 
 * @author fmichel
*/

public class ProcessMonthlyStats
{
	private static Logger logger = Logger.getLogger(ProcessMonthlyStats.class.getName());

	private static Configuration config = Config.getConfiguration();

	/** File where to write the distribution of photos by post times over 24 hours */
	private static PrintStream psTimeDistrib;

	/** File where to write the "time to explore" average and max values */
	private static PrintStream psTimeT2E;

	/** File where to write the users average number of photos and contacts */
	private static PrintStream psUserAvg;

	/** File where to write the distribution of uses by number of photos */
	private static PrintStream psUserDistribPhoto;

	/** File where to write the distribution of uses by number of contacts */
	private static PrintStream psUserDistribContact;

	/** File where to write the groups results */
	private static PrintStream psGroup;

	/** File where to write the tags results */
	private static PrintStream psTag;

	/** File where to write the uploads results */
	private static PrintStream psUploads;

	/** File where to write the distribution of photos by nb of groups */
	private static PrintStream psActivDistribGroup;

	/** File where to write the distribution of photos by nb of views */
	private static PrintStream psActivDistribViews;

	/** File where to write the distribution of photos by nb of comments */
	private static PrintStream psActivDistribComments;

	/** File where to write the distribution of photos by nb of favs */
	private static PrintStream psActivDistribFavs;

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
			TimeStat.initComputeMonthlyPostTimeDistrib(psTimeDistrib);

			psTimeT2E = new PrintStream(config.getString("fm.flickr.stat.time.dir") + "/monthly_times_t2e.csv");
			TimeStat.initComputeMonthlyT2E(psTimeT2E);
		}

		if (config.getString("fm.flickr.stat.action.user").equals("on")) {
			psUserAvg = new PrintStream(config.getString("fm.flickr.stat.user.dir") + "/monthly_user_average.csv");
			UserStat.initComputeMonthlyAvg(psUserAvg);

			psUserDistribPhoto = new PrintStream(config.getString("fm.flickr.stat.user.dir") + "/monthly_user_distrib_photo.csv");
			UserStat.initComputeMonthlyDistrib(psUserDistribPhoto, config.getInt("fm.flickr.stat.user.distrib.group.slice"), config.getInt("fm.flickr.stat.user.distrib.nbslices"));

			psUserDistribContact = new PrintStream(config.getString("fm.flickr.stat.user.dir") + "/monthly_user_distrib_contact.csv");
			UserStat.initComputeMonthlyDistrib(psUserDistribContact, config.getInt("fm.flickr.stat.user.distrib.contact.slice"), config.getInt("fm.flickr.stat.user.distrib.nbslices"));
		}

		if (config.getString("fm.flickr.stat.action.group").equals("on")) {
			psGroup = new PrintStream(config.getString("fm.flickr.stat.group.dir") + "/monthly_results.csv");
			GroupStat.initComputeMonthly(psGroup);
		}

		if (config.getString("fm.flickr.stat.action.tag").equals("on")) {
			psTag = new PrintStream(config.getString("fm.flickr.stat.tag.dir") + "/monthly_results.csv");
			TagStat.initComputeMonthly(psTag);
		}

		if (config.getString("fm.flickr.stat.action.uploads").equals("on")) {
			psUploads = new PrintStream(config.getString("fm.flickr.stat.uploads.dir") + "/monthly_results.csv");
			DailyUploadsStat.initComputeMonthly(psUploads);
		}

		if (config.getString("fm.flickr.stat.action.activity").equals("on")) {
			psActivDistribGroup = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_group.csv");
			ActivityStat.initComputeMonthlyDistrib(psActivDistribGroup, config.getInt("fm.flickr.stat.activity.distrib.group.slice"), config.getInt("fm.flickr.stat.activity.distrib.group.nbslices"));

			psActivDistribViews = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_view.csv");
			ActivityStat.initComputeMonthlyDistrib(psActivDistribViews, config.getInt("fm.flickr.stat.activity.distrib.view.slice"), config.getInt("fm.flickr.stat.activity.distrib.view.nbslices"));

			psActivDistribComments = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_comment.csv");
			ActivityStat.initComputeMonthlyDistrib(psActivDistribComments, config.getInt("fm.flickr.stat.activity.distrib.comment.slice"), config.getInt("fm.flickr.stat.activity.distrib.comment.nbslices"));

			psActivDistribFavs = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_fav.csv");
			ActivityStat.initComputeMonthlyDistrib(psActivDistribFavs, config.getInt("fm.flickr.stat.activity.distrib.fav.slice"), config.getInt("fm.flickr.stat.activity.distrib.fav.nbslices"));
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
			TimeStat.loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.user").equals("on"))
			UserStat.loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			GroupStat.loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			TagStat.loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			DailyUploadsStat.loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.activity").equals("on"))
			ActivityStat.loadFilesByMonth(yearMonth);
	}

	/**
	 * Run the specific statistics processings 
	 * @param ps the output where to print results
	 * @param month a string denoting the current month formatted as yyyy-mm
	 */
	private static void computeStatistics(String month) {

		if (config.getString("fm.flickr.stat.action.time").equals("on")) {
			TimeStat.computeMonthlyPostTimeDistrib(psTimeDistrib, month);
			TimeStat.computeMonthlyT2E(psTimeT2E, month);
		}

		if (config.getString("fm.flickr.stat.action.user").equals("on")) {
			UserStat.computeMonthlyAvg(psUserAvg, month);
			UserStat.computeMonthlyDistribPhoto(psUserDistribPhoto, month);
			UserStat.computeMonthlyDistribContact(psUserDistribContact, month);
		}

		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			GroupStat.computeMonthlyStatistics(psGroup, month);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			TagStat.computeMonthlyStatistics(psTag, month);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			DailyUploadsStat.computeMonthlyStatistics(psUploads, month);

		if (config.getString("fm.flickr.stat.action.activity").equals("on")) {
			ActivityStat.computeMonthlyDistribGroup(psActivDistribGroup, month);
			ActivityStat.computeMonthlyDistribViews(psActivDistribViews, month);
			ActivityStat.computeMonthlyDistribComments(psActivDistribComments, month);
			ActivityStat.computeMonthlyDistribFavs(psActivDistribFavs, month);
		}
	}
}
