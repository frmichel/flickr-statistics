package fm.flickr.stat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.service.FlickrService;
import fm.flickr.api.wrapper.service.param.PhotoItem;
import fm.flickr.api.wrapper.service.param.PhotoItemsSet;
import fm.flickr.stat.perform.ActivityStat;
import fm.flickr.stat.perform.UploadsStat;
import fm.flickr.stat.perform.GroupStat;
import fm.flickr.stat.perform.TagStat;
import fm.util.Config;
import fm.util.Util;

/**
 * <p>This is the main entry point for collecting data. It gets photo ids either from Interestingness at the 
 * specified dates, or from an input file that was built previously.</p>
 * <p>For each photo id, it collects additinal stats about groups, tags, users, times and activity,
 * using classes from package fm.flickr.stat.perform.
 * In addition, it can also collect the number of photos uploaded to Flickr hour by hour. </p>
 * @author fmichel
*/
public class CollectPhotosData
{
	private static Logger logger = Logger.getLogger(CollectPhotosData.class.getName());

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

			//--- Collect data from Interstingness and store it into one file per day and per type of statistics
			while (calStart.before(calEnd)) {

				// Format date to process as yyyy-mm-dd
				String date = sdf.format(calStart.getTime());
				logger.info("Starting collecting data on " + date);

				// Collect data on photos on that date (In case of Interstingness, max 500 photos are reported every day)
				collect(date);

				// Collect number of daily uploads to Flickr
				if (config.getString("fm.flickr.stat.action.uploads").equals("on"))
					new UploadsStat().collecDailyUploads(date);

				// Increase the date by n days, and proceed with that next date
				calStart.add(GregorianCalendar.DAY_OF_MONTH, config.getInt("fm.flickr.stat.step_between_measure"));

				// Sleep between each photo... just not to be overloading (may not be necessary...)
				try {
					//logger.info("Sleep for 5s");
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
	 * <p>Retrieve a list of photo ids either from Interestingness or from a file, then retrieve data
	 * about each photo, depending on properties fm.flickr.stat.action.*.</p>
	 * <p>When ids are retrieved from Interestingness, a maximum of 'fm.flickr.stat.maxphotos' photos 
	 * will be processed every day.</p>
	 * <p>The results are saved into files by the classes implementing statistics.</p>
	 * 
	 * @param date date of Interestingness from which photos are taken, given in format "YYY-MM-DD"
	 */
	private static void collect(String date) throws IOException {
		PhotoItemsSet photos = null;

		if (config.getString("fm.flickr.stat.action.group").equals("on") || config.getString("fm.flickr.stat.action.tag").equals("on") || config.getString("fm.flickr.stat.action.activity").equals("on") || config.getString("fm.flickr.stat.action.anyphoto").equals("on")) {

			String photoList = System.getProperty("fm.flickr.stat.photoslist");
			if (photoList != null) {
				photos = getPhotoIdsFromFile(photoList);
			} else {
				// Get photos from Interstingness on the given date (Flickr will report maximum 500 every day)
				photos = service.getPhotoIdsFromInterestingness(date, config.getInt("fm.flickr.stat.maxphotos"), 1);
			}

			if (photos == null || photos.size() == 0) {
				logger.warn("######## " + date + ": 0 photo to be processed.");
			} else {
				logger.info("######## " + date + ": " + photos.size() + " photos to be processed...");

				if (config.getString("fm.flickr.stat.action.group").equals("on"))
					GroupStat.collecAdditionalData(date, photos);

				if (config.getString("fm.flickr.stat.action.tag").equals("on"))
					TagStat.collecAdditionalData(date, photos);

				if (config.getString("fm.flickr.stat.action.activity").equals("on")) {
					File outputFile = new File(Util.getDir(config.getString("fm.flickr.stat.activity.dir")), date + ".csv");
					ActivityStat.collecAdditionalData(outputFile, date, photos);
				}

				if (config.getString("fm.flickr.stat.action.anyphoto").equals("on")) {
					File outputFile = new File(Util.getDir(config.getString("fm.flickr.stat.anyphoto.dir")), date + ".csv");
					ActivityStat.collecAdditionalData(outputFile, date, photos);
				}
			}
		}
	}

	/**
	 * This specific function is a way of running the statistics acquisition process not on a list of
	 * of photo ids retrieved from Interestingness, but from a simple list of photo identifiers given in a file.
	 * 
	 * @param fileName the file were the photos identifiers are listed in the interestingness rank order
	 * @return the set of photos, or null if an error occurs (IO error or file not found)
	 */
	private static PhotoItemsSet getPhotoIdsFromFile(String fileName) {

		ArrayList<PhotoItem> photosList = new ArrayList<PhotoItem>();
		File file = null;
		int rank = 1;

		try {
			file = new File(fileName);
			if (!file.exists()) {
				String errMsg = "No file: " + file.getAbsolutePath();
				logger.warn(errMsg);
				return null;
			}

			FileInputStream fis = new FileInputStream(file);
			BufferedReader buffer = new BufferedReader(new InputStreamReader(fis));
			logger.info("Loading photo ids from file " + file.getAbsolutePath());

			String photoId = buffer.readLine();
			while (photoId != null) {
				if (!photoId.isEmpty()) {
					// Create a simple PhotItem with only the photo id and the interestingness rank
					PhotoItem item = new PhotoItem();
					item.setPhotoId(photoId);
					item.setInterestingnessRank(rank++);
					photosList.add(item);
				}
				photoId = buffer.readLine();
			}
			fis.close();
			return new PhotoItemsSet(photosList, 1, photosList.size());

		} catch (IOException e) {
			String errMsg = "Error when reading file " + file.getName() + ". Exception: " + e.toString();
			logger.warn(errMsg);
			return null;
		}
	}
}
