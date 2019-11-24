package fm.flickr.stat;

import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Vector;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.util.ServiceException;
import fm.flickr.stat.perform.ActivityStat;
import fm.flickr.stat.perform.UploadsStat;
import fm.util.Config;

/** 
 * This class crosses data of two sources: post time of explored photos, and total number of uploads,
 * in order to compute the ratio of the number of explored photos / number of photos posted,
 * broken down by post time (0 to 23h) AND week day.
 *  
 * @author fmichel
*/

public class ComputeProbabilityPerWeekDayAndHour
{
	private static Logger logger = Logger.getLogger(ComputeProbabilityPerWeekDayAndHour.class.getName());

	private static Configuration config = Config.getConfiguration();

	private static Vector<Vector<Integer>> exploredTable = new Vector<Vector<Integer>>();

	/** Number of uploaded photos by day and hour: Matrix of 7 days x 24 hours: */
	private static Vector<Vector<Long>> uploadTable = new Vector<Vector<Long>>();

	/** Activity about explored photos */
	private static ActivityStat activity = null;

	/**
	 * Initialize the table of "probability per day of way and per hour"
	 */
	private static void initStats() {
		for (int day = 0; day < 7; day++) {
			exploredTable.add(new Vector<Integer>());
			uploadTable.add(new Vector<Long>());
			for (int hour = 0; hour < 24; hour++) {
				exploredTable.get(day).add(0);
				uploadTable.get(day).add((long) 0);
			}
		}
	}

	public static void main(String[] args) {

		SimpleDateFormat dateFrmt = new SimpleDateFormat("yyyy-MM-dd");
		logger.debug("begin");

		initStats();

		try {
			// Turn start and stop dates into GregorianCalendars 
			String startDate = config.getString("fm.flickr.stat.startdate");
			String[] tokensStart = startDate.split("-");
			GregorianCalendar calStart = new GregorianCalendar(Integer.valueOf(tokensStart[0]), Integer.valueOf(tokensStart[1]) - 1, Integer.valueOf(tokensStart[2]));

			String stopDate = config.getString("fm.flickr.stat.enddate");
			String[] tokensEnd = stopDate.split("-");
			GregorianCalendar calEnd = new GregorianCalendar(Integer.valueOf(tokensEnd[0]), Integer.valueOf(tokensEnd[1]) - 1, Integer.valueOf(tokensEnd[2]));

			PrintStream output = new PrintStream("/proba_explo.csv");

			//--- Load all daily data files created between start date and end date
			calStart = new GregorianCalendar(Integer.valueOf(tokensStart[0]), Integer.valueOf(tokensStart[1]) - 1, Integer.valueOf(tokensStart[2]));
			output.println("# YYY-MM-DD HH:MM; nb of explored photos; nb of uploads; % of explored/posted");
			while (calStart.before(calEnd)) {
				// Format date to process as yyyy-mm-dd
				String date = dateFrmt.format(calStart.getTime());
				int dayOfWeek = convertUsDoWToFr(calStart.get(GregorianCalendar.DAY_OF_WEEK));

				try {
					loadFileByDay(date);
					computeStatistics(dayOfWeek);
				} catch (ServiceException e) {
					logger.warn(e.toString());
				}
				// Increase the date by 1 day, and proceed with that next date
				calStart.add(GregorianCalendar.DAY_OF_MONTH, 1);
			}

			// Display final table of probabilities
			computeProba(output);
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

		// Load the Activity files
		activity = new ActivityStat();
		activity.loadFileByDay(date, config.getString("fm.flickr.stat.activity.dir"));

		// Load the upload file
		UploadsStat.reset();
		UploadsStat.loadFileByDay(date);
	}

	/**
	 * 
	 * Distribute the data loaded about upload time and number of uploads into 2 tables 
	 * of the same data summed by date and time slot
	 * @param dayOfWeek 1=Sunday to 7=Saturday 
	 */
	private static void computeStatistics(int dayOfWeek) throws ServiceException {

		Vector<Integer> postTimeDistrib = activity.getPostTimeDistrib();
		Vector<Long> uploadDistrib = UploadsStat.getUploadDistribution();

		for (int hour = 0; hour < 24; hour++) {

			int curVal = exploredTable.get(dayOfWeek).get(hour);
			exploredTable.get(dayOfWeek).set(hour, curVal + postTimeDistrib.get(hour));

			long curValL = uploadTable.get(dayOfWeek).get(hour);
			uploadTable.get(dayOfWeek).set(hour, curValL + uploadDistrib.get(hour));
		}
	}

	/**
	 * @param ps the output where to print results
	 */
	private static void computeProba(PrintStream ps) {
		ps.println("; 00h; 01h; 02h; 03h; 04h; 05h; 06h; 07h; 08h; 09h; 10h; 11h; 12h; 13h; 14h; 15h; 16h; 17h; 18h; 19h; 20h; 21h; 22h; 23h");

		for (int day = 0; day < 7; day++) {
			ps.print(getDayName(day) + "; ");

			for (int hour = 0; hour < 24; hour++) {
				
				if (uploadTable.get(day).get(hour) != 0) { // avoid division by 0
					float f = exploredTable.get(day).get(hour) * 100;
					f = f / uploadTable.get(day).get(hour);
					ps.printf("%2.4f", f);
				} else
					ps.printf("%2.4f", (float) 0);
				if (hour < 23)
					ps.print("; ");
			}
			ps.println();
		}
	}

	/** 
	* Convert a US day of week (1=Sunday to 7=Saturday) to a French day of week starting at 0
	* (0=Monday to 6=Sunday)
	* @param usDoW US day of Week from 1 to 7
	* @return converted day of week (0 to 6)
	*/
	private static int convertUsDoWToFr(int usDoW) {
		return (usDoW + 5) % 7;
	}

	private static String getDayName(int doW) {
		switch (doW) {
		case 0:
			return "monday";
		case 1:
			return "tuesday";
		case 2:
			return "wednesday";
		case 3:
			return "thursday";
		case 4:
			return "friday";
		case 5:
			return "saturday";
		case 6:
			return "sunday";
		default:
			return "unknown";
		}
	}
}
