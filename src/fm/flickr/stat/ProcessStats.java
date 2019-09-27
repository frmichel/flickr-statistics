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
 * This main class runs the specific treatments to provide statistics results over a period of time.
 * All results are displayed to the std output.
 * 
 * @author fmichel
*/

public class ProcessStats
{
	private static Logger logger = Logger.getLogger(ProcessStats.class.getName());

	private static Configuration config = Config.getConfiguration();

	/** Activity about explored photos */
	private ActivityStat activExpld = new ActivityStat();

	/** Activity about all (non-explored) photos */
	private ActivityStat activAll = new ActivityStat();

	/** Activity about all (non-explored) photos one month after post */
	private ActivityStat activAll1Month = new ActivityStat();

	/** Streams where to write result of activity about explored and unexplored photos */
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

	/** Streams where to write result of activity about explored and unexplored photos */
	private PrintStreams streams = new PrintStreams();

	public static void main(String[] args) {
		try {
			SimpleDateFormat dateFrmt = new SimpleDateFormat("yyyy-MM-dd");
			ProcessStats processor = new ProcessStats();
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
			processor.computeStatistics();

			logger.debug("end");

		} catch (Exception e) {
			logger.error("Unexpected error. Exception: " + e.toString());
			e.printStackTrace();
		}
	}

	private void initComputeActivity() throws FileNotFoundException {

		streams.groups = System.out;

		streams.tags = System.out;

		if (config.getString("fm.flickr.stat.action.uploads").equals("on")) {
			streams.uploads = new PrintStream(config.getString("fm.flickr.stat.uploads.dir") + "/distrib_results.csv");
			UploadsStat.initComputeDistribUploads(streams.uploads);
		}

		streams.distribGroup = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_group.csv");
		activExpld.initComputeDistrib(streams.distribGroup, config.getInt("fm.flickr.stat.activity.distrib.group.slice"), config.getInt("fm.flickr.stat.activity.distrib.group.nbslices"));

		streams.distribViews = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_view.csv");
		activExpld.initComputeDistrib(streams.distribViews, config.getInt("fm.flickr.stat.activity.distrib.view.slice"), config.getInt("fm.flickr.stat.activity.distrib.view.nbslices"));

		streams.distribComments = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_comment.csv");
		activExpld.initComputeDistrib(streams.distribComments, config.getInt("fm.flickr.stat.activity.distrib.comment.slice"), config.getInt("fm.flickr.stat.activity.distrib.comment.nbslices"));

		streams.distribFavs = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_fav.csv");
		activExpld.initComputeDistrib(streams.distribFavs, config.getInt("fm.flickr.stat.activity.distrib.fav.slice"), config.getInt("fm.flickr.stat.activity.distrib.fav.nbslices"));

		streams.distribTags = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_tag.csv");
		activExpld.initComputeDistrib(streams.distribTags, config.getInt("fm.flickr.stat.activity.distrib.tag.slice"), config.getInt("fm.flickr.stat.activity.distrib.tag.nbslices"));

		streams.distribOwnersPhotos = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_owners_photo.csv");
		activExpld.initComputeDistrib(streams.distribOwnersPhotos, config.getInt("fm.flickr.stat.activity.distrib.user_photo.slice"), config.getInt("fm.flickr.stat.activity.distrib.user.nbslices"));

		streams.distribOwnersContacts = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_owners_contact.csv");
		activExpld.initComputeDistrib(streams.distribOwnersContacts, config.getInt("fm.flickr.stat.activity.distrib.user_contact.slice"), config.getInt("fm.flickr.stat.activity.distrib.user.nbslices"));

		streams.distribLocation = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_location.csv");
		activExpld.initComputeDistribLocation(streams.distribLocation);

		streams.timeDistrib = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_time.csv");
		activExpld.initComputeDistribPostTime(streams.timeDistrib);

		streams.userAvg = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/user_average.csv");
		activExpld.initComputeUserStat(streams.userAvg);
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

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			UploadsStat.loadFileByDay(date);

		if (config.getString("fm.flickr.stat.action.activity").equals("on"))
			activExpld.loadFileByDay(date, config.getString("fm.flickr.stat.activity.dir"));

		if (config.getString("fm.flickr.stat.action.anyphoto").equals("on")) {
			activAll.loadFileByDay(date, config.getString("fm.flickr.stat.anyphoto.dir"));
			activAll1Month.loadFileByDay(date, config.getString("fm.flickr.stat.anyphoto_1monthago.dir"));
		}
	}

	/**
	 * Run the specific statistics processing
	 * 
	 * @param ps output stream
	 */
	private void computeStatistics() {
		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			GroupStat.displayGroupsByPopularity(streams.groups);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			TagStat.displayTagsByPopularity(streams.tags);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			UploadsStat.computeDistribUploads(streams.uploads);

		if (config.getString("fm.flickr.stat.action.activity").equals("on")) {
			logger.info("Computing statistincs of about activity on explored photos");
			activExpld.computeDistribGroup(streams.distribGroup, "explored");
			activExpld.computeDistribViews(streams.distribViews, "explored");
			activExpld.computeDistribComments(streams.distribComments, "explored");
			activExpld.computeDistribFavs(streams.distribFavs, "explored");
			activExpld.computeDistribTags(streams.distribTags, "explored");
			activExpld.computeDistribOwnersPhotos(streams.distribOwnersPhotos, "explored");
			activExpld.computeDistribOwnersContacts(streams.distribOwnersContacts, "explored");
			activExpld.computeDistribLocation(streams.distribLocation, "explored");
			activExpld.computeDistribPostTime(streams.timeDistrib, "explored");
			activExpld.computeUserStat(streams.userAvg, "explored");
		}

		// Stats on non-explored photos: stats are calculated the day we collected the data. 
		// We also compute stats on the number of views, favs, comments one month after the photo was posted.
		if (config.getString("fm.flickr.stat.action.anyphoto").equals("on")) {
			logger.info("Computing statistincs of about activity on non-explored photos");
			activAll.computeDistribGroup(streams.distribGroup, "all");
			activAll1Month.computeDistribGroup(streams.distribGroup, "all 1 month old");

			activAll.computeDistribViews(streams.distribViews, "all");
			activAll1Month.computeDistribViews(streams.distribViews, "all 1 month old");

			activAll.computeDistribComments(streams.distribComments, "all");
			activAll1Month.computeDistribComments(streams.distribComments, "all 1 month old");

			activAll.computeDistribFavs(streams.distribFavs, "all");
			activAll1Month.computeDistribFavs(streams.distribFavs, "all 1 month old");

			activAll.computeDistribTags(streams.distribTags, "all");
			activAll.computeDistribOwnersPhotos(streams.distribOwnersPhotos, "all");
			activAll.computeDistribOwnersContacts(streams.distribOwnersContacts, "all");
			activAll.computeDistribLocation(streams.distribLocation, "all");
			activAll.computeDistribPostTime(streams.timeDistrib, "all");
			activAll.computeUserStat(streams.userAvg, "all");
		}
	}
}
