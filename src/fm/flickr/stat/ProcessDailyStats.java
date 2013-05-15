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
 * This main class runs the specific treatments to provide statistics results over a period of time.
 * All results are displyed to the std output.
 * 
 * @author fmichel
*/

public class ProcessDailyStats
{
	private static Logger logger = Logger.getLogger(ProcessDailyStats.class.getName());

	private static Configuration config = Config.getConfiguration();

	/** Activity about explored photos */
	private ActivityStat activExpld = new ActivityStat();

	/** Activity about all (non-explored) photos */
	private ActivityStat activAll = new ActivityStat();

	/** Activity about all (non-explored) photos one month after post */
	private ActivityStat activAll1Month = new ActivityStat();

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
	}

	/** Streams where to write result of activiy about explored and unexplored photos */
	private PsActivity psActivity = new PsActivity();

	public static void main(String[] args) {
		try {
			SimpleDateFormat dateFrmt = new SimpleDateFormat("yyyy-MM-dd");
			ProcessDailyStats processor = new ProcessDailyStats();
			if (config.getString("fm.flickr.stat.action.activity").equals("on") || config.getString("fm.flickr.stat.action.anyphoto").equals("on"))
				processor.initComputeActivity();

			logger.debug("begin");

			// Turn start and stop dates into GregorianCalendars 
			String startDate = config.getString("fm.flickr.stat.startdate");
			String[] tokensStart = startDate.split("-");
			GregorianCalendar calStart = new GregorianCalendar(Integer.valueOf(tokensStart[0]), Integer.valueOf(tokensStart[1]) - 1, Integer.valueOf(tokensStart[2]));

			String stopDate = config.getString("fm.flickr.stat.enddate");
			String[] tokensEnd = stopDate.split("-");
			GregorianCalendar calEnd = new GregorianCalendar(Integer.valueOf(tokensEnd[0]), Integer.valueOf(tokensEnd[1]) - 1, Integer.valueOf(tokensEnd[2]));

			//--- Load all daily data files created between start date and end date
			calStart = new GregorianCalendar(Integer.valueOf(tokensStart[0]), Integer.valueOf(tokensStart[1]) - 1, Integer.valueOf(tokensStart[2]));
			while (calStart.before(calEnd)) {
				// Format date to process as yyyy-mm-dd
				String date = dateFrmt.format(calStart.getTime());
				try {
					processor.loadFileByDay(date);
				} catch (ServiceException e) {
					logger.warn(e.toString());
				}
				// Increase the date by 1 day, and proceed with that next date
				calStart.add(GregorianCalendar.DAY_OF_MONTH, 1);
			}

			//--- Comupute and write the results to the output
			System.out.println("# Period from " + startDate + " to " + stopDate + "");
			processor.computeDailyStatistics(System.out);

			logger.debug("end");

		} catch (Exception e) {
			logger.error("Unexpected error. Exception: " + e.toString());
			e.printStackTrace();
		}
	}

	private void initComputeActivity() throws FileNotFoundException {

		psActivity.distribGroup = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_group.csv");
		activExpld.initComputeDistrib(psActivity.distribGroup, config.getInt("fm.flickr.stat.activity.distrib.group.slice"), config.getInt("fm.flickr.stat.activity.distrib.group.nbslices"));

		psActivity.distribViews = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_view.csv");
		activExpld.initComputeDistrib(psActivity.distribViews, config.getInt("fm.flickr.stat.activity.distrib.view.slice"), config.getInt("fm.flickr.stat.activity.distrib.view.nbslices"));

		psActivity.distribComments = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_comment.csv");
		activExpld.initComputeDistrib(psActivity.distribComments, config.getInt("fm.flickr.stat.activity.distrib.comment.slice"), config.getInt("fm.flickr.stat.activity.distrib.comment.nbslices"));

		psActivity.distribFavs = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_fav.csv");
		activExpld.initComputeDistrib(psActivity.distribFavs, config.getInt("fm.flickr.stat.activity.distrib.fav.slice"), config.getInt("fm.flickr.stat.activity.distrib.fav.nbslices"));

		psActivity.distribTags = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_tag.csv");
		activExpld.initComputeDistrib(psActivity.distribTags, config.getInt("fm.flickr.stat.activity.distrib.tag.slice"), config.getInt("fm.flickr.stat.activity.distrib.tag.nbslices"));

		psActivity.distribOwnersPhotos = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_owners_photo.csv");
		activExpld.initComputeDistrib(psActivity.distribOwnersPhotos, config.getInt("fm.flickr.stat.activity.distrib.user_photo.slice"), config.getInt("fm.flickr.stat.activity.distrib.user.nbslices"));

		psActivity.distribOwnersContacts = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_owners_contact.csv");
		activExpld.initComputeDistrib(psActivity.distribOwnersContacts, config.getInt("fm.flickr.stat.activity.distrib.user_contact.slice"), config.getInt("fm.flickr.stat.activity.distrib.user.nbslices"));
	}

	/**
	 * Runs the loading of data files on the given date by the dedicated statistics classes
	 * 
	 * @param date 
	 * @throws IOException
	 */
	private void loadFileByDay(String date) throws ServiceException {
		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			GroupStat.loadFileByDay(date);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			TagStat.loadFileByDay(date);

		if (config.getString("fm.flickr.stat.action.time").equals("on"))
			TimeStat.loadFileByDay(date);

		if (config.getString("fm.flickr.stat.action.user").equals("on"))
			UserStat.loadFileByDay(date);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			DailyUploadsStat.loadFileByDay(date);

		if (config.getString("fm.flickr.stat.action.activity").equals("on"))
			activExpld.loadFileByDay(date, config.getString("fm.flickr.stat.activity.dir"));

		if (config.getString("fm.flickr.stat.action.anyphoto").equals("on")) {
			activAll.loadFileByDay(date, config.getString("fm.flickr.stat.anyphoto.dir"));
			activAll1Month.loadFileByDay(date, config.getString("fm.flickr.stat.anyphoto_1monthago.dir"));
		}
	}

	/**
	 * Run the specific statistics processings 
	 * @param ps the output where to print results
	 */
	private void computeDailyStatistics(PrintStream ps) {
		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			GroupStat.computeStatistics(ps);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			TagStat.computeStatistics(ps);

		if (config.getString("fm.flickr.stat.action.time").equals("on"))
			TimeStat.computeStatistics(ps);

		if (config.getString("fm.flickr.stat.action.user").equals("on"))
			UserStat.computeStatistics(ps);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			DailyUploadsStat.computeStatistics(ps);

		if (config.getString("fm.flickr.stat.action.activity").equals("on")) {
			logger.info("Computing statistincs of about activity on explored photos");
			activExpld.computeDistribGroup(psActivity.distribGroup, "explored");
			activExpld.computeDistribViews(psActivity.distribViews, "explored");
			activExpld.computeDistribComments(psActivity.distribComments, "explored");
			activExpld.computeDistribFavs(psActivity.distribFavs, "explored");
			activExpld.computeDistribTags(psActivity.distribTags, "explored");
			activExpld.computeDistribOwnersPhotos(psActivity.distribOwnersPhotos, "explored");
			activExpld.computeDistribOwnersContacts(psActivity.distribOwnersContacts, "explored");
		}

		if (config.getString("fm.flickr.stat.action.anyphoto").equals("on")) {
			logger.info("Computing statistincs of about activity on non-explored photos");
			activAll.computeDistribGroup(psActivity.distribGroup, "all 1 day old");
			activAll1Month.computeDistribGroup(psActivity.distribGroup, "all 1 month old");

			activAll.computeDistribViews(psActivity.distribViews, "all 1 day old");
			activAll1Month.computeDistribViews(psActivity.distribViews, "all 1 month old");

			activAll.computeDistribComments(psActivity.distribComments, "all 1 day old");
			activAll1Month.computeDistribComments(psActivity.distribComments, "all 1 month old");

			activAll.computeDistribFavs(psActivity.distribFavs, "all 1 day old");
			activAll1Month.computeDistribFavs(psActivity.distribFavs, "all 1 month old");

			activAll.computeDistribTags(psActivity.distribTags, "all 1 day old");
			activAll1Month.computeDistribTags(psActivity.distribTags, "all 1 month old");

			activAll.computeDistribOwnersPhotos(psActivity.distribOwnersPhotos, "all 1 day old");
			activAll1Month.computeDistribOwnersPhotos(psActivity.distribOwnersPhotos, "all 1 month old");

			activAll.computeDistribOwnersContacts(psActivity.distribOwnersContacts, "all 1 day old");
			activAll1Month.computeDistribOwnersContacts(psActivity.distribOwnersContacts, "all 1 month old");
		}
	}
}
