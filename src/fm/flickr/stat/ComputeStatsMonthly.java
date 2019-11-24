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
 * This main class runs the specific treatments to provide statistic results broken down per month.
 * 
 * @author fmichel
*/

public class ComputeStatsMonthly extends ComputeStats
{
	private static Logger logger = Logger.getLogger(ComputeStatsMonthly.class.getName());

	public static void main(String[] args) {
		try {
			logger.debug("begin");
			ComputeStatsMonthly processor = new ComputeStatsMonthly();
			processor.initComputeActivity();

			//--- Process all data files created per month and per type of statistics
			SimpleDateFormat ymDateFrmt = new SimpleDateFormat("yyyy-MM");
			while (processor.calStart.before(processor.calEnd)) {

				// Format month to process as yyyy-mm and load the files of that month
				String month = ymDateFrmt.format(processor.calStart.getTime());
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
				processor.calStart.add(GregorianCalendar.MONTH, 1);
			}

			logger.debug("end");

		} catch (Exception e) {
			logger.error("Unexpected error. Exception: " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Run the specific statistics processings
	 * 
	 * @param month a string denoting the current month formatted as yyyy-mm
	 */
	private void computeStatistics(String month) {

		if (config.getString("fm.flickr.stat.action.group").equals("on"))
			GroupStat.computeMonthlyStatistics(streams.groupsDistrib, month);

		if (config.getString("fm.flickr.stat.action.tag").equals("on"))
			TagStat.computeMonthlyStatistics(streams.tagsDistrib, month);

		if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
			UploadsStat.computeDistribUploads(streams.uploads, month);

		if (config.getString("fm.flickr.stat.action.activity").equals("on")) {
			activityStat.computeDistribGroup(streams.distribGroup, month);
			activityStat.computeDistribViews(streams.distribViews, month);
			activityStat.computeDistribComments(streams.distribComments, month);
			activityStat.computeDistribFavs(streams.distribFavs, month);
			activityStat.computeDistribTags(streams.distribTags, month);
			activityStat.computeDistribOwnersPhotos(streams.distribOwnersPhotos, month);
			activityStat.computeDistribOwnersContacts(streams.distribOwnersContacts, month);
			activityStat.computeDistribLocation(streams.distribLocation, month);
			activityStat.computeDistribPostTime(streams.timeDistrib, month);
			activityStat.computeUserStat(streams.userAvg, month);
		}
	}
}
