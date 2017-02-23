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
import fm.flickr.stat.perform.UploadsStat;
import fm.flickr.stat.perform.GroupStat;
import fm.flickr.stat.perform.TagStat;
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

	/** Streams where to write result of activiy about explored and unexplored photos */
	private class PrintStreams
	{
		/** File where to write the groups results */
		PrintStream groups;

		/** File where to write the tags results */
		PrintStream tags;

		/** File where to write the results of the total number of uploads */
		PrintStream uploads;

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

		/** File where to write the distribution of photos by post times over 24 hours */
		PrintStream timeDistrib;

		/** File where to write the users average number of photos and contacts */
		PrintStream userAvg;
	}

	/** Activity about explored photos */
	private ActivityStat activExpld = new ActivityStat();

	/** Streams where to write result of activiy about explored and unexplored photos */
	private PrintStreams streams = new PrintStreams();

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

		if (config.getString("fm.flickr.stat.action.group").equals("on")) {
			streams.groups = new PrintStream(config.getString("fm.flickr.stat.group.dir") + "/monthly_results.csv");
			GroupStat.initComputeMonthly(streams.groups);
		}

		if (config.getString("fm.flickr.stat.action.tag").equals("on")) {
			streams.tags = new PrintStream(config.getString("fm.flickr.stat.tag.dir") + "/monthly_results.csv");
			TagStat.initComputeMonthly(streams.tags);
		}

		if (config.getString("fm.flickr.stat.action.uploads").equals("on")) {
			streams.uploads = new PrintStream(config.getString("fm.flickr.stat.uploads.dir") + "/monthly_results.csv");
			UploadsStat.initComputeDistribUploads(streams.uploads);
		}

		if (config.getString("fm.flickr.stat.action.activity").equals("on")) {
			streams.distribGroup = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_group.csv");
			activExpld.initComputeDistrib(streams.distribGroup, config.getInt("fm.flickr.stat.activity.distrib.group.slice"), config.getInt("fm.flickr.stat.activity.distrib.group.nbslices"));

			streams.distribViews = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_view.csv");
			activExpld.initComputeDistrib(streams.distribViews, config.getInt("fm.flickr.stat.activity.distrib.view.slice"), config.getInt("fm.flickr.stat.activity.distrib.view.nbslices"));

			streams.distribComments = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_comment.csv");
			activExpld.initComputeDistrib(streams.distribComments, config.getInt("fm.flickr.stat.activity.distrib.comment.slice"), config.getInt("fm.flickr.stat.activity.distrib.comment.nbslices"));

			streams.distribFavs = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_fav.csv");
			activExpld.initComputeDistrib(streams.distribFavs, config.getInt("fm.flickr.stat.activity.distrib.fav.slice"), config.getInt("fm.flickr.stat.activity.distrib.fav.nbslices"));

			streams.distribTags = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_tag.csv");
			activExpld.initComputeDistrib(streams.distribTags, config.getInt("fm.flickr.stat.activity.distrib.tag.slice"), config.getInt("fm.flickr.stat.activity.distrib.tag.nbslices"));

			streams.distribOwnersPhotos = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_owners_photo.csv");
			activExpld.initComputeDistrib(streams.distribOwnersPhotos, config.getInt("fm.flickr.stat.activity.distrib.user_photo.slice"), config.getInt("fm.flickr.stat.activity.distrib.user.nbslices"));

			streams.distribOwnersContacts = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_owners_contact.csv");
			activExpld.initComputeDistrib(streams.distribOwnersContacts, config.getInt("fm.flickr.stat.activity.distrib.user_contact.slice"), config.getInt("fm.flickr.stat.activity.distrib.user.nbslices"));

			streams.distribLocation = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_location.csv");
			activExpld.initComputeDistribLocation(streams.distribLocation);

			streams.timeDistrib = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_distrib_time.csv");
			activExpld.initComputeDistribPostTime(streams.timeDistrib);

			streams.userAvg = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/monthly_user_average.csv");
			activExpld.initComputeUserStat(streams.userAvg);
		}
	}

	/**
	 * Runs the loading of data files on the given moth by the dedicated statistics classes
	 * 
	 * @param yearMonth year and month formatted as yyyy-mm
	 * @throws IOException
	 */
	private void loadFileByMonth(String yearMonth) throws ServiceException {

		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			GroupStat.loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			TagStat.loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			UploadsStat.loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.activity").equals("on"))
			activExpld.loadFilesByMonth(yearMonth, config.getString("fm.flickr.stat.activity.dir"));
	}

	/**
	 * Run the specific statistics processings 
	 * @param ps the output where to print results
	 * @param month a string denoting the current month formatted as yyyy-mm
	 */
	private void computeStatistics(String month) {

		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			GroupStat.computeMonthlyStatistics(streams.groups, month);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			TagStat.computeMonthlyStatistics(streams.tags, month);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			UploadsStat.computeDistribUploads(streams.uploads, month);

		if (config.getString("fm.flickr.stat.action.activity").equals("on")) {
			activExpld.computeDistribGroup(streams.distribGroup, month);
			activExpld.computeDistribViews(streams.distribViews, month);
			activExpld.computeDistribComments(streams.distribComments, month);
			activExpld.computeDistribFavs(streams.distribFavs, month);
			activExpld.computeDistribTags(streams.distribTags, month);
			activExpld.computeDistribOwnersPhotos(streams.distribOwnersPhotos, month);
			activExpld.computeDistribOwnersContacts(streams.distribOwnersContacts, month);
			activExpld.computeDistribLocation(streams.distribLocation, month);
			activExpld.computeDistribPostTime(streams.timeDistrib, month);
			activExpld.computeUserStat(streams.userAvg, month);
		}
	}
}
