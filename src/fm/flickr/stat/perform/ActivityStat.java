package fm.flickr.stat.perform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.service.FlickrService;
import fm.flickr.api.wrapper.service.param.PhotoItem;
import fm.flickr.api.wrapper.service.param.PhotoItemInfo;
import fm.flickr.api.wrapper.service.param.PhotoItemsSet;
import fm.flickr.api.wrapper.util.ServiceException;
import fm.flickr.stat.IStat;
import fm.util.Config;
import fm.util.Util;

/**
 * Manage the information about the actibity on photos: number of comments, of notes, of favorites
 * @author fmichrl
 *
 */
public class ActivityStat implements IStat
{
	private static Logger logger = Logger.getLogger(ActivityStat.class.getName());

	private static Configuration config = Config.getConfiguration();

	private static FlickrService service = new FlickrService();

	private static HashMap<Integer, PhotoItemInfo> statistics = new HashMap<Integer, PhotoItemInfo>();
	
	private static String FIELD_SEPARATOR = ";";
	
	/**
	 * <p>Retrieve the number favs of photos from Interestingness. The results are saved to a file.</p>
	 * 
	 * @param date date of photos from Interestingness, given in format "YYY-MM-DD"
	 * @param photos photos retrieved from Interestingness
	 * @throws IOException in case the file can't be saved
	 */
	@Override
	public void collecAdditionalData(String date, PhotoItemsSet photos) throws IOException {

		HashMap<Integer, PhotoItemInfo> stats = new HashMap<Integer, PhotoItemInfo>();

		int nbPhotosProcessed = 0; // Number of photos explored during the whole process

		// Loop on all photos retrieved at once from Interstingness
		for (PhotoItem photo : photos.getPhotosList()) {
			nbPhotosProcessed++;

			// Read photo infos inluding nb of comments and notes
			PhotoItemInfo info = service.getPhotoInfo(photo.getPhotoId());
			if (info != null) {
				info.setInterestingnessRank(photo.getInterestingnessRank());

				// Read the number of favs
				info.setNbFavs(service.getNbFavs(photo.getPhotoId()));
				stats.put(info.getInterestingnessRank(), info);
			}

			// Trace activity every 10 photos
			if (nbPhotosProcessed % 10 == 0)
				logger.info("Processed " + nbPhotosProcessed + " photos.");

			// Sleep between each photo... just not to be overloading
			try {
				Thread.sleep(config.getInt("fm.flickr.stat.sleepms"));
			} catch (InterruptedException e) {
				logger.warn("Unepected interruption: " + e.toString());
				e.printStackTrace();
			}
		}

		logger.info("### Processed " + stats.size() + " photos");

		saveActivityFromInterestingPhotos(date, stats, nbPhotosProcessed);
	}

	/**
	 * Save the information into a file
	 * 
	 * @param date
	 * @param stats list of photos information
	 * @param nbPhotosProcessed Number of photos explored during the whole process
	 * @throws IOException
	 */
	private static void saveActivityFromInterestingPhotos(String date, HashMap<Integer, PhotoItemInfo> stats, int nbPhotosProcessed) throws IOException {

		File file = new File(Util.getDir(config.getString("fm.flickr.stat.activity.dir")), date + ".csv");
		FileOutputStream fos = new FileOutputStream(file);
		PrintWriter writer = new PrintWriter(fos);
		logger.info("Saving activity info about " + stats.size() + " photos into file " + file.getCanonicalPath());

		writer.println("# Number of photos processed: " + nbPhotosProcessed);
		writer.println("rank ; views ; comments ; favs ; notes");

		Collection<PhotoItemInfo> photoItemInfo = stats.values();
		Iterator<PhotoItemInfo> iter = photoItemInfo.iterator();
		while (iter.hasNext()) {
			PhotoItemInfo entry = iter.next();
			writer.println(entry.getInterestingnessRank() + FIELD_SEPARATOR + entry.getNbViews() + FIELD_SEPARATOR + entry.getNbComments() + FIELD_SEPARATOR + entry.getNbFavs() + FIELD_SEPARATOR + entry.getNbNotes());
		}
		writer.close();
		fos.close();
	}

	/**
	 * Load the content of the file for the given date, into the map of statistics
	 * 
	 * @param date date of data collected from Interestingness, given in format "YYY-MM-DD"
	 */
	@Override
	public void loadFileByDay(String date) throws ServiceException {
		String fileName = config.getString("fm.flickr.stat.activity.dir") + date + ".log";
		loadFile(new File(fileName));
		logger.info("### " + statistics.size() + " photos activity loaded.");
	}

	/**
	 * Load the content of the all files for the given month, into the map of statistics
	 * 
	 * @param yearMonth year and month formatted as yyyy-mm
	 */
	@Override
	public void loadFilesByMonth(String yearMonth) throws ServiceException {
		// Empty the current data if any
		statistics.clear();

		File dir = new File(config.getString("fm.flickr.stat.activity.dir"));
		if (!dir.exists() || !dir.isDirectory()) {
			String errMsg = "Error: data directory " + config.getString("fm.flickr.stat.activity.dir") + " does not exists.";
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}

		for (File file : dir.listFiles())
			if (file.getName().startsWith(yearMonth))
				loadFile(file);

		logger.info("### " + statistics.size() + " total photos activity loaded for period " + yearMonth);
	}

	/** 
	* Parse the content of the given file and store its content into the static map statistics
	* @param file  
	*/
	private void loadFile(File file) throws ServiceException {
		try {
			if (!file.exists()) {
				logger.warn("No file: " + file.getAbsolutePath());
				return;
			}

			FileInputStream fis = new FileInputStream(file);
			BufferedReader buffer = new BufferedReader(new InputStreamReader(fis));

			logger.info("### Loading file " + file.getAbsolutePath());

			String str = buffer.readLine();
			while (str != null) {
				if (!str.trim().startsWith("#")) { // ignore comment lines stating with #

					String[] tokens = str.split(PhotoItemInfo.FIELD_SEPARATOR);
					if (tokens.length != 6)
						logger.warn("Wrong format on line: " + str);
					else {
						PhotoItemInfo inf = new PhotoItemInfo();
						inf.setInterestingnessRank(Integer.valueOf(tokens[0]));
						inf.setPhotoId(tokens[1]);
						inf.setNbViews(tokens[2]);
						inf.setNbComments(tokens[3]);
						inf.setNbFavs(tokens[4]);
						inf.setNbNotes(tokens[5]);

						statistics.put(inf.getInterestingnessRank(), inf);
					}
				}
				str = buffer.readLine();
			}
			fis.close();

		} catch (IOException e) {
			String errMsg = "Error when reading file " + file.getName() + ". Exception: " + e.toString();
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}
	}

	/**
	 * 
	 * @param ps where to print the output
	 */
	@Override
	public void computeStatistics(PrintStream ps) {
	}

	public void initComputeMonthly(PrintStream ps) throws FileNotFoundException {
	}

	/**
	 * 
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. May be null.
	 */
	public void computeMonthlyStatistics(PrintStream ps, String month) {
	}
}
