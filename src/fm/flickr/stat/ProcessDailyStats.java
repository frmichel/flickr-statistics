package fm.flickr.stat;

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
 * This main class runs the specific treatments to provide statistics results over a period of time.
 * All results are displyed to the std output.
 * 
 * @author fmichel
*/

public class ProcessDailyStats
{
	private static Logger logger = Logger.getLogger(ProcessDailyStats.class.getName());

	private static Configuration config = Config.getConfiguration();

	public static void main(String[] args) {

		SimpleDateFormat dateFrmt = new SimpleDateFormat("yyyy-MM-dd");
		logger.debug("begin");

		try {
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
					loadFileByDay(date);
				} catch (ServiceException e) {
					logger.warn(e.toString());
				}
				// Increase the date by 1 day, and proceed with that next date
				calStart.add(GregorianCalendar.DAY_OF_MONTH, 1);
			}

			//--- Comupute and write the results to the output
			computeDailyStatistics(System.out);

			logger.debug("end");

		} catch (Exception e) {
			logger.error("Unexpected error. Exception: " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Runs the loading of data files on the given date by the dedicated statistics classes
	 * 
	 * @param date 
	 * @throws IOException
	 */
	private static void loadFileByDay(String date) throws ServiceException {
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
	}

	/**
	 * Run the specific statistics processings 
	 * @param ps the output where to print results
	 */
	private static void computeDailyStatistics(PrintStream ps) {
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
	}
}
