package fm.flickr.stat.perform;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.GregorianCalendar;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.util.ServiceException;
import fm.util.Config;

/** 
 * @author fmichel
*/

public class ComputeStats
{
	protected Logger logger = Logger.getLogger(ComputeStats.class.getName());

	protected Configuration config;

	protected String startDate;

	protected GregorianCalendar calStart;

	protected String stopDate;

	protected GregorianCalendar calEnd;

	/** Streams where to write result of activity about explored and unexplored photos */
	protected PrintStreams streams = new PrintStreams();

	/** Activity about explored photos */
	protected ActivityStat activityStat = new ActivityStat();

	public ComputeStats() {
		config = Config.getConfiguration();

		// Turn start and stop dates into GregorianCalendars 
		startDate = config.getString("fm.flickr.stat.startdate");
		String[] tokensStart = startDate.split("-");
		calStart = new GregorianCalendar(Integer.valueOf(tokensStart[0]), Integer.valueOf(tokensStart[1]) - 1, Integer.valueOf(tokensStart[2]));

		stopDate = config.getString("fm.flickr.stat.enddate");
		String[] tokensEnd = stopDate.split("-");
		calEnd = new GregorianCalendar(Integer.valueOf(tokensEnd[0]), Integer.valueOf(tokensEnd[1]) - 1, Integer.valueOf(tokensEnd[2]));
	}

	/** Streams where to write result of activity about explored and unexplored photos */
	protected class PrintStreams
	{
		/** File where to write the groups list */
		public PrintStream groupsSortedByHit;

		/** File where to write the results of photos distributed by number of groups */
		public PrintStream groupsDistrib;

		/** File where to write the tags list */
		public PrintStream tagsSortedByHit;

		/** File where to write the results of photos distributed by number of tags */
		public PrintStream tagsDistrib;

		/** File where to write the results of the total number of uploads */
		public PrintStream uploads;

		/** File where to write the distribution of photos by nb of groups */
		public PrintStream distribGroup;

		/** File where to write the distribution of photos by nb of views */
		public PrintStream distribViews;

		/** File where to write the distribution of photos by nb of comments */
		public PrintStream distribComments;

		/** File where to write the distribution of photos by nb of favs */
		public PrintStream distribFavs;

		/** File where to write the distribution of photos by nb of tags */
		public PrintStream distribTags;

		/** File where to write the distribution of photos by total nb of photos of the owner */
		public PrintStream distribOwnersPhotos;

		/** File where to write the distribution of photos by total nb of contacts of the owner */
		public PrintStream distribOwnersContacts;

		/** File where to write the distribution of photos by wether they have location or not */
		public PrintStream distribLocation;

		/** File where to write the distribution of photos by post times over 24 hours */
		public PrintStream timeDistrib;

		/** File where to write the users average number of photos and contacts */
		public PrintStream userAvg;
	}

	protected void initComputeActivity() throws FileNotFoundException {

		if (config.getString("fm.flickr.stat.action.group").equals("on")) {
			streams.groupsSortedByHit = new PrintStream(config.getString("fm.flickr.stat.group.dir") + "/result_sorted_listed.txt");
			streams.groupsDistrib = new PrintStream(config.getString("fm.flickr.stat.group.dir") + "/result_avg.csv");
			GroupStat.initComputeMonthly(streams.groupsDistrib);
		}

		if (config.getString("fm.flickr.stat.action.tag").equals("on")) {
			streams.tagsSortedByHit = new PrintStream(config.getString("fm.flickr.stat.tag.dir") + "/result_sorted_listed.txt");
			streams.tagsDistrib = new PrintStream(config.getString("fm.flickr.stat.tag.dir") + "/result_avg.csv");
			TagStat.initComputeMonthly(streams.tagsDistrib);
		}

		if (config.getString("fm.flickr.stat.action.uploads").equals("on")) {
			streams.uploads = new PrintStream(config.getString("fm.flickr.stat.uploads.dir") + "/distrib_results.csv");
			UploadsStat.initComputeDistribUploads(streams.uploads);
		}

		streams.distribGroup = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_group.csv");
		activityStat.initComputeDistrib(streams.distribGroup, config.getInt("fm.flickr.stat.activity.distrib.group.slice"), config.getInt("fm.flickr.stat.activity.distrib.group.nbslices"));

		streams.distribViews = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_view.csv");
		activityStat.initComputeDistrib(streams.distribViews, config.getInt("fm.flickr.stat.activity.distrib.view.slice"), config.getInt("fm.flickr.stat.activity.distrib.view.nbslices"));

		streams.distribComments = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_comment.csv");
		activityStat.initComputeDistrib(streams.distribComments, config.getInt("fm.flickr.stat.activity.distrib.comment.slice"), config.getInt("fm.flickr.stat.activity.distrib.comment.nbslices"));

		streams.distribFavs = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_fav.csv");
		activityStat.initComputeDistrib(streams.distribFavs, config.getInt("fm.flickr.stat.activity.distrib.fav.slice"), config.getInt("fm.flickr.stat.activity.distrib.fav.nbslices"));

		streams.distribTags = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_tag.csv");
		activityStat.initComputeDistrib(streams.distribTags, config.getInt("fm.flickr.stat.activity.distrib.tag.slice"), config.getInt("fm.flickr.stat.activity.distrib.tag.nbslices"));

		streams.distribOwnersPhotos = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_owners_photo.csv");
		activityStat.initComputeDistrib(streams.distribOwnersPhotos, config.getInt("fm.flickr.stat.activity.distrib.user_photo.slice"), config.getInt("fm.flickr.stat.activity.distrib.user.nbslices"));

		streams.distribOwnersContacts = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_owners_contact.csv");
		activityStat.initComputeDistrib(streams.distribOwnersContacts, config.getInt("fm.flickr.stat.activity.distrib.user_contact.slice"), config.getInt("fm.flickr.stat.activity.distrib.user.nbslices"));

		streams.distribLocation = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_location.csv");
		activityStat.initComputeDistribLocation(streams.distribLocation);

		streams.timeDistrib = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/distrib_time.csv");
		activityStat.initComputeDistribPostTime(streams.timeDistrib);

		streams.userAvg = new PrintStream(config.getString("fm.flickr.stat.activity.dir") + "/user_average.csv");
		activityStat.initComputeUserStat(streams.userAvg);
	}

	/**
	 * Runs the loading of data files on the given date by the dedicated statistics classes
	 * 
	 * @param date 
	 * @throws IOException
	 */
	protected void loadFileByDay(String date) throws ServiceException {

		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			GroupStat.loadFileByDay(date);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			TagStat.loadFileByDay(date);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			UploadsStat.loadFileByDay(date);

		if (config.getString("fm.flickr.stat.action.activity").equals("on"))
			activityStat.loadFileByDay(date, config.getString("fm.flickr.stat.activity.dir"));
	}

	/**
	 * Runs the loading of data files on the given moth by the dedicated statistics classes
	 * 
	 * @param yearMonth year and month formatted as yyyy-mm
	 * @throws IOException
	 */
	protected void loadFileByMonth(String yearMonth) throws ServiceException {

		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			GroupStat.loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			TagStat.loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			UploadsStat.loadFilesByMonth(yearMonth);

		if (config.getString("fm.flickr.stat.action.activity").equals("on"))
			activityStat.loadFilesByMonth(yearMonth, config.getString("fm.flickr.stat.activity.dir"));
	}
}
