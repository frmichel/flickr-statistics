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

	/** File where to write the users average number of photos and contacts */
	private static PrintStream psUserAvg;

	/** File where to write the groups results */
	private static PrintStream psGroup;

	/** File where to write the tags results */
	private static PrintStream psTag;

	/** File where to write the results of the total number of uploads */
	private static PrintStream psUploads;

	/** Streams where to write result of activiy about explored and unexplored photos */
	static class PsActivity
	{
		/** File where to write the distribution of photos by nb of groups */
		PrintStream distribGroup;

		/** File where to write the distribution of photos by nb of views */
		PrintStream distribViews;

		/** File where to write the distribution of photos by nb of comments */
		PrintStream distribComments;

		/** File where to write the distribution of photos by nb of favs */
		PrintStream distribFavs;

		/** File where to write the distribution of photos by nb of tags */
		PrintStream distribTags;

		/** File where to write the distribution of photos by total nb of photos of the owner */
		PrintStream distribOwnersPhotos;

		/** File where to write the distribution of photos by total nb of contacts of the owner */
		PrintStream distribOwnersContacts;

		/** File where to write the distribution of photos by wether they have location or not */
		PrintStream distribLocation;
	}

	/** Activity about explored photos */
	private ActivityStat activExpld = new ActivityStat();

	/** Streams where to write result of activiy about explored and unexplored photos */
	private PsActivity psActivity = new PsActivity();

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
			ProcessMonthlyStats processor = new ProcessMonthlyStats();
			processor.initComputeByMonth();

			//--- Process all files created per month and per type of statistics
			calStart = new GregorianCalendar(Integer.valueOf(tokensStart[0]), Integer.valueOf(tokensStart[1]) - 1, Integer.valueOf(tokensStart[2]));
			while (calStart.before(calEnd)) {

				// Format month to process as yyyy-mm and load the files of that month
				String month = ymDateFrmt.format(calStart.getTime());
				logger.info("Processing data for month " + month);
				try {
					// Load all data files for the given month 
					processor.loadFileByMonth(month);

					// Comupute and write results into output files
					processor.computeStatistics(month);

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
	private void initComputeByMonth() throws FileNotFoundException {

		if (config.getString("fm.flickr.stat.action.time").equals("on")) {
			psTimeDistrib = new PrintStream(config.getString("fm.flickr.stat.time.dir") + "/monthly_times_distrib.csv");
			TimeStat.initComputeMonthlyPostTimeDistrib(psTimeDistrib);
		}

		if (config.getString("fm.flickr.stat.action.user").equals("on")) {
			psUserAvg = new PrintStream(config.getString("fm.flickr.stat.user.dir") + "/monthly_user_average.csv");
			UserStat.initComputeMonthlyAvg(psUserAvg);
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
			psActivity.distribGroup = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_group.csv");
			activExpld.initComputeDistrib(psActivity.distribGroup, config.getInt("fm.flickr.stat.activity.distrib.group.slice"), config.getInt("fm.flickr.stat.activity.distrib.group.nbslices"));

			psActivity.distribViews = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_view.csv");
			activExpld.initComputeDistrib(psActivity.distribViews, config.getInt("fm.flickr.stat.activity.distrib.view.slice"), config.getInt("fm.flickr.stat.activity.distrib.view.nbslices"));

			psActivity.distribComments = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_comment.csv");
			activExpld.initComputeDistrib(psActivity.distribComments, config.getInt("fm.flickr.stat.activity.distrib.comment.slice"), config.getInt("fm.flickr.stat.activity.distrib.comment.nbslices"));

			psActivity.distribFavs = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_fav.csv");
			activExpld.initComputeDistrib(psActivity.distribFavs, config.getInt("fm.flickr.stat.activity.distrib.fav.slice"), config.getInt("fm.flickr.stat.activity.distrib.fav.nbslices"));

			psActivity.distribTags = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_tag.csv");
			activExpld.initComputeDistrib(psActivity.distribTags, config.getInt("fm.flickr.stat.activity.distrib.tag.slice"), config.getInt("fm.flickr.stat.activity.distrib.tag.nbslices"));

			psActivity.distribOwnersPhotos = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_owners_photo.csv");
			activExpld.initComputeDistrib(psActivity.distribOwnersPhotos, config.getInt("fm.flickr.stat.activity.distrib.user_photo.slice"), config.getInt("fm.flickr.stat.activity.distrib.user.nbslices"));

			psActivity.distribOwnersContacts = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_owners_contact.csv");
			activExpld.initComputeDistrib(psActivity.distribOwnersContacts, config.getInt("fm.flickr.stat.activity.distrib.user_contact.slice"), config.getInt("fm.flickr.stat.activity.distrib.user.nbslices"));

			psActivity.distribLocation = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_location.csv");
			psActivity.distribLocation.println("# ; yes; no");
		}
	}

	/**
	 * Runs the loading of data files on the given moth by the dedicated statistics classes
	 * 
	 * @param yearMonth year and month formatted as yyyy-mm
	 * @throws IOException
	 */
	private void loadFileByMonth(String yearMonth) throws ServiceException {

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
			activExpld.loadFilesByMonth(yearMonth, config.getString("fm.flickr.stat.activity.dir"));
	}

	/**
	 * Run the specific statistics processings 
	 * @param ps the output where to print results
	 * @param month a string denoting the current month formatted as yyyy-mm
	 */
	private void computeStatistics(String month) {

		if (config.getString("fm.flickr.stat.action.time").equals("on"))
			TimeStat.computeMonthlyPostTimeDistrib(psTimeDistrib, month);

		if (config.getString("fm.flickr.stat.action.user").equals("on"))
			UserStat.computeMonthlyAvg(psUserAvg, month);

		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			GroupStat.computeMonthlyStatistics(psGroup, month);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			TagStat.computeMonthlyStatistics(psTag, month);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			DailyUploadsStat.computeMonthlyStatistics(psUploads, month);

		if (config.getString("fm.flickr.stat.action.activity").equals("on")) {
			activExpld.computeDistribGroup(psActivity.distribGroup, month);
			activExpld.computeDistribViews(psActivity.distribViews, month);
			activExpld.computeDistribComments(psActivity.distribComments, month);
			activExpld.computeDistribFavs(psActivity.distribFavs, month);
			activExpld.computeDistribTags(psActivity.distribTags, month);
			activExpld.computeDistribOwnersPhotos(psActivity.distribOwnersPhotos, month);
			activExpld.computeDistribOwnersContacts(psActivity.distribOwnersContacts, month);
			activExpld.computeDistribLocation(psActivity.distribLocation, month);
		}
	}
}
