package fm.flickr.stat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.service.FlickrService;
import fm.flickr.api.wrapper.service.param.PhotoItemsSet;
import fm.flickr.stat.perform.ActivityStat;
import fm.util.Config;
import fm.util.Util;

/**
 * This is the main entry point for collecting data on any photo posted to flickr. The point of getting such data is to
 * compate it with photos retrieved from Interestingness, and find out what's specific of photos from Interestingness, 
 * and what is just commonalities of most photos on Flickr, may they be in Interestingness or not.
 * 
 * This main will use properties:<ul>
 * <li>fm.flickr.stat.startdate</li>
 * <li>fm.flickr.stat.enddate</li>
 * <li>fm.flickr.stat.step_between_measure</li>
 * <li>fm.flickr.stat.sleepms</li>
 * <li>fm.flickr.stat.anyphoto.nbphotos.</li></ul>
 * It does not take into account properties fm.flickr.stat.action.'*', but basically applies the activity statistics.
 * 
 * @author fmichel
*/
public class CollectAnyPhotoData
{
	private static Logger logger = Logger.getLogger(CollectAnyPhotoData.class.getName());

	private static Configuration config = Config.getConfiguration();

	/** Wrapper for Flickr services */
	private static FlickrService service = new FlickrService();

	public static void main(String[] args) {

		logger.debug("begin");
		try {
			// Convert start and stop dates into GregorianCalendars
			String startDate = config.getString("fm.flickr.stat.startdate");
			String[] tokensStart = startDate.split("-");
			GregorianCalendar calStart = new GregorianCalendar(Integer.valueOf(tokensStart[0]), Integer.valueOf(tokensStart[1]) - 1, Integer.valueOf(tokensStart[2]));

			String stopDate = config.getString("fm.flickr.stat.enddate");
			String[] tokensEnd = stopDate.split("-");
			GregorianCalendar calEnd = new GregorianCalendar(Integer.valueOf(tokensEnd[0]), Integer.valueOf(tokensEnd[1]) - 1, Integer.valueOf(tokensEnd[2]));

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

			//--- Collect data and store it into daily files
			while (calStart.before(calEnd)) {

				// Format date to process as yyyy-mm-dd
				String date = sdf.format(calStart.getTime());
				logger.info("Starting collecting data on " + date);

				// Collect data on random photos on that date
				collectData(date);

				// Increase the date by n days, and proceed with that next date
				calStart.add(GregorianCalendar.DAY_OF_MONTH, config.getInt("fm.flickr.stat.step_between_measure"));

				// Sleep between each photo... just not to be overloading (may not be necessary...)
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					logger.warn("Unepected interruption: " + e.toString());
					e.printStackTrace();
				}
			}

			logger.info("end");

		} catch (Exception e) {
			logger.error("Unexpected error. Exception: " + e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * <p>Retrieve a random list of photos on the given date, a total of 'fm.flickr.stat.anyphoto.nbphotos' photos 
	 * are retrieved. The results are saved into one csv file per day.</p>
	 * 
	 * @param date date of photos from Interestingness, given in format "YYY-MM-DD"
	 */
	private static void collectData(String date) throws IOException {

		// Get random photos on the given date
		PhotoItemsSet photos = service.getRandomPhotos(date, config.getInt("fm.flickr.stat.anyphoto.nbphotos"));
		if (photos == null || photos.size() == 0) {
			logger.warn("######## " + date + ": 0 photo randomly retrieved on date " + date);
		} else {
			logger.info("######## " + date + ": " + photos.size() + " photos randomly retrieved");
			File outputFile = new File(Util.getDir(config.getString("fm.flickr.stat.anyphoto.dir")), date + ".csv");
			ActivityStat.collecAdditionalData(outputFile, date, photos);
		}
	}
}
