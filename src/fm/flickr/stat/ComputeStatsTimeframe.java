package fm.flickr.stat;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.util.ServiceException;
import fm.flickr.stat.perform.ComputeStats;
import fm.flickr.stat.perform.GroupStat;
import fm.flickr.stat.perform.TagStat;
import fm.flickr.stat.perform.UploadsStat;

/** 
 * This main class runs the specific treatments to provide statistic results over a period of time.
 * 
 * @author fmichel
*/

public class ComputeStatsTimeframe extends ComputeStats
{
	protected static Logger logger = Logger.getLogger(ComputeStatsTimeframe.class.getName());

	private static ComputeStatsTimeframe processor;

	public ComputeStatsTimeframe() {
		super();
	}

	public static void main(String[] args) {
		try {
			logger.debug("begin");
			processor = new ComputeStatsTimeframe();
			processor.initComputeActivity();

			//--- Load all daily data files created between start date and end date per type of statistics
			SimpleDateFormat dateFrmt = new SimpleDateFormat("yyyy-MM-dd");
			while (processor.calStart.before(processor.calEnd)) {

				// Format date to process as yyyy-mm-dd
				String date = dateFrmt.format(processor.calStart.getTime());
				try {
					processor.loadFileByDay(date);
				} catch (ServiceException e) {
					logger.warn(e.toString());
				}
				// Increase the date by 1 day, and proceed with that next date
				processor.calStart.add(GregorianCalendar.DAY_OF_MONTH, 1);
			}

			//--- Comupute and write the results to the output
			System.out.println("# Period from " + processor.startDate + " to " + processor.stopDate + "");
			processor.computeStatistics();

			logger.debug("end");

		} catch (Exception e) {
			logger.error("Unexpected error. Exception: " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Run the specific statistics processing
	 */
	private void computeStatistics() {

		if (config.getString("fm.flickr.stat.action.group").equals("on")) {
			GroupStat.displayGroupsByPopularity(streams.groupsSortedByHit);
			GroupStat.computeMonthlyStatistics(streams.groupsDistrib, "Period from " + processor.startDate + " to " + processor.stopDate);
		}

		if (config.getString("fm.flickr.stat.action.tag").equals("on")) {
			TagStat.displayTagsByPopularity(streams.tagsSortedByHit);
			TagStat.computeMonthlyStatistics(streams.tagsDistrib, "Period from " + processor.startDate + " to " + processor.stopDate);
		}

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			UploadsStat.computeDistribUploads(streams.uploads);

		if (config.getString("fm.flickr.stat.action.activity").equals("on")) {
			activityStat.computeDistribGroup(streams.distribGroup, "explored");
			activityStat.computeDistribViews(streams.distribViews, "explored");
			activityStat.computeDistribComments(streams.distribComments, "explored");
			activityStat.computeDistribFavs(streams.distribFavs, "explored");
			activityStat.computeDistribTags(streams.distribTags, "explored");
			activityStat.computeDistribOwnersPhotos(streams.distribOwnersPhotos, "explored");
			activityStat.computeDistribOwnersContacts(streams.distribOwnersContacts, "explored");
			activityStat.computeDistribLocation(streams.distribLocation, "explored");
			activityStat.computeDistribPostTime(streams.timeDistrib, "explored");
			activityStat.computeUserStat(streams.userAvg, "explored");
		}
	}
}
