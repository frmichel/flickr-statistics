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
import fm.util.Config;

/** 
 * This specific main class simply reports what already exists in daily files, namely the total number of photos
 * uploaded every day, and stores the result in file daily_uploads.csv.
 * 
 * @author fmichel
*/

public class ProcessUploadsPerDay
{
	private static Logger logger = Logger.getLogger(ProcessUploadsPerDay.class.getName());

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
			
			PrintStream ps = new PrintStream(config.getString("fm.flickr.stat.uploads.dir") + "/daily_uploads.csv");

			calStart = new GregorianCalendar(Integer.valueOf(tokensStart[0]), Integer.valueOf(tokensStart[1]) - 1, Integer.valueOf(tokensStart[2]));
			System.out.println("# YYY-MM-DD HH:MM; nb of explored photos; nb of uploads; % of explored/posted");
			while (calStart.before(calEnd)) {
				// Format date to process as yyyy-mm-dd
				String date = dateFrmt.format(calStart.getTime());
				try {
					loadFileByDay(date);
					computeStatistics(date, ps);

				} catch (ServiceException e) {
					logger.warn(e.toString());
				}
				// Increase the date by 1 day, and proceed with that next date
				calStart.add(GregorianCalendar.DAY_OF_MONTH, 1);
			}

			logger.debug("end");
			ps.close();

		} catch (Exception e) {
			logger.error("Unexpected error. Exception: " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * @param date 
	 * @throws IOException
	 */
	private static void loadFileByDay(String date) throws ServiceException {
		DailyUploadsStat.reset();
		DailyUploadsStat.loadFileByDay(date);
	}

	/**
	 * @param ps the output where to print results
	 */
	private static void computeStatistics(String date, PrintStream ps) {
		Vector<Long> uploadDistrib = DailyUploadsStat.getUploadDistribution();
		ps.println(date + ";" + uploadDistrib.get(24));
	}
}
