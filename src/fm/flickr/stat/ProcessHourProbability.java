package fm.flickr.stat;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.util.ServiceException;
import fm.flickr.stat.perform.DailyUploadsStat;
import fm.flickr.stat.perform.TimeStat;
import fm.util.Config;

/** 
 * This specific class crosses data of two sources: post time of explored photos, and total number of uploads,
 * in order to compute the probability of being explored given the post time
 *  
 * @author fmichel
*/

public class ProcessHourProbability
{
	private static Logger logger = Logger.getLogger(ProcessHourProbability.class.getName());

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
			System.out.println("# YYY-MM-DD HH:MM; nb of explored photos; nb of uploads; % of explored/posted");
			while (calStart.before(calEnd)) {
				// Format date to process as yyyy-mm-dd
				String date = dateFrmt.format(calStart.getTime());
				try {
					loadFileByDay(date);
					computeStatistics(date, System.out);

				} catch (ServiceException e) {
					logger.warn(e.toString());
				}
				// Increase the date by 1 day, and proceed with that next date
				calStart.add(GregorianCalendar.DAY_OF_MONTH, 1);
			}

			logger.debug("end");

		} catch (Exception e) {
			logger.error("Unexpected error. Exception: " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Reset the static maps, and load data files (post time and uploads) on the given date
	 * 
	 * @param date 
	 * @throws IOException
	 */
	private static void loadFileByDay(String date) throws ServiceException {
		TimeStat.reset();
		DailyUploadsStat.reset();

		TimeStat.loadFileByDay(date);
		DailyUploadsStat.loadFileByDay(date);
	}

	/**
	 * Run the specific statistics processings 
	 * @param ps the output where to print results
	 */
	private static void computeStatistics(String date, PrintStream ps) {

		Vector<Integer> postTimeDistrib = TimeStat.getPostTimeDistrib();
		Vector<Long> uploadDistrib = DailyUploadsStat.getUploadDistribution();

		// Print the results cut down by hour of day, from 0h to 23h
		// Output format is: YYY-MM-DD HH:MM; nb of explored photos; nb of uploads; % of explored/posted  
		for (int i = 0; i < 24; i++) {
			ps.print(date + " " + i + ":00:00; ");
			ps.print(postTimeDistrib.get(i) + "; ");
			ps.print(uploadDistrib.get(i) + "; ");
			float f = postTimeDistrib.get(i) * 100;
			f = f / uploadDistrib.get(i);
			ps.printf("%2.4f", f);
			ps.println();
		}
	}
}
