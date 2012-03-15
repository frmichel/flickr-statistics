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
 * @author fmichel
*/

public class ProcessDailyTimeAndUpload
{
	private static Logger logger = Logger.getLogger(ProcessDailyTimeAndUpload.class.getName());

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
	 * Runs the loading of data files on the given date
	 * 
	 * @param date 
	 * @throws IOException
	 */
	private static void loadFileByDay(String date) throws ServiceException {
		TimeStat.reset();
		DailyUploadsStat.reset();

		new TimeStat().loadFileByDay(date);
		new DailyUploadsStat().loadFileByDay(date);
	}

	/**
	 * Run the specific statistics processings 
	 * @param ps the output where to print results
	 */
	private static void computeStatistics(String date, PrintStream ps) {

		Vector<Integer> postTimeDistrib = new TimeStat().getPostTimeDistrib();
		Vector<Long> uploadDistrib = new DailyUploadsStat().getUploadDistribution();

		// Print the results cut down by hour of day, from 0h to 23h
		for (int i = 0; i < 24; i++) {
			ps.print(date + " " + i + ":00:00; ");
			ps.print(postTimeDistrib.get(i) + "; ");
			ps.print(uploadDistrib.get(i));
			ps.println();
		}

	}
}
