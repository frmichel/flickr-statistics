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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;

import fm.flickr.api.wrapper.service.FlickrService;
import fm.flickr.api.wrapper.service.param.PhotoItem;
import fm.flickr.api.wrapper.service.param.PhotoItemsSet;
import fm.flickr.api.wrapper.service.param.TagItem;
import fm.flickr.api.wrapper.service.param.TagItemsSet;
import fm.flickr.api.wrapper.util.ServiceException;
import fm.flickr.stat.param.TagItemStat;
import fm.flickr.stat.param.TagsPerPhoto;
import fm.util.Config;
import fm.util.Util;

/**
 * Collect the number of tag of explored photos.
 * 
 * Daily stats load result files over a period and sorts tags by number of occurences, ie. the number of 
 * explored photos that have each tag.
 * 
 * Monthly stats will compute the average and max number of tag of a photo.
 * 
 * @author Atreyu
 */
public class TagStat
{
	private static Logger logger = Logger.getLogger(TagStat.class.getName());

	private static Configuration config = Config.getConfiguration();

	private static FlickrService service = new FlickrService();

	private static HashMap<String, TagItemStat> statistics = new HashMap<String, TagItemStat>();

	/** Stats of number of tags a photo has */
	private static List<TagsPerPhoto> statisticsTpP = new ArrayList<TagsPerPhoto>();

	/**
	 * <p>Retrieve the list of tags of photos from Interestingness, and counts the number
	 * of occurences of each tag.</p>
	 * <p>A maximum of 'fm.flickr.stat.tag.maxtags' tags will be stored.</p>
	 * <p>The results are saved to a file.</p>
	 * 
	 * @param date date of photos from Interestingness, given in format "YYY-MM-DD"
	 * @param photos photos retrieved from Interestingness
	 * @throws IOException in case the file can't be saved
	 */
	public static void collecAdditionalData(String date, PhotoItemsSet photos) throws IOException {

		HashMap<String, TagItemStat> stats = new HashMap<String, TagItemStat>();

		int nbPhotosProcessed = 0; // Number of photos explored during the whole process
		int sumTags = 0; // Sum of the number of tags of each photo (to compute the average)
		int maxTags = 0; // max number of tag of a photo
		Vector<Integer> listNumTags = new Vector<Integer>(); // list of number of tags of each photos (to compute the dtd deviation)

		// Loop on all photos retrieved at once from Interstingness
		for (PhotoItem photo : photos.getPhotosList()) {
			nbPhotosProcessed++;

			// Get all tags of the photo
			TagItemsSet tagSet = service.getPhotoTags(photo.getPhotoId());

			// For each tag check if it is already in the list, add it if it is not
			if (tagSet != null) {
				if (tagSet.size() > maxTags)
					maxTags = tagSet.size();
				listNumTags.add(tagSet.size());

				// For each tag check if it is already in the list, add it if it is not
				for (TagItem tag : tagSet.getTagsList()) {
					sumTags++;
					String tagId = tag.getTagId();

					TagItemStat tagStat = stats.get(tagId);
					if (tagStat != null) {
						// Either increment the number of occurences by one if the tag already exists
						tagStat.incNbOccurences();
						logger.trace("Updating tag " + tagId + ": " + tagStat.getNbOccurences());
					} else {
						// Or create a new tag element with 1 occurence, but limit the maximum number of tags
						if (stats.size() < config.getInt("fm.flickr.stat.tag.maxtags")) {
							stats.put(tagId, new TagItemStat(tag, 1));
							logger.trace("Adding tag " + tagId);
						}
					}
				}

				// Trace activity every 10 photos
				if (nbPhotosProcessed % 10 == 0)
					logger.info("Processed " + nbPhotosProcessed + " photos, registered " + stats.size() + " tags.");
			}

			// Sleep between each photo... just not to be overloading
			try {
				Thread.sleep(config.getInt("fm.flickr.stat.sleepms"));
			} catch (InterruptedException e) {
				logger.warn("Unepected interruption: " + e.toString());
				e.printStackTrace();
			}

		}

		logger.info("### Processed " + stats.size() + " tags from " + nbPhotosProcessed + " photos");
		logger.info("### Average number of tags per photo: " + (new Float(sumTags)) / nbPhotosProcessed);
		logger.info("### Maximum number of tags per photo: " + maxTags);

		// Calculate the standard deviation
		float avg = sumTags / nbPhotosProcessed;
		float sumDeviations = 0;
		for (Integer nbtag : listNumTags)
			sumDeviations += Math.abs((new Float(nbtag)) - avg);

		float stdDev = 0;
		if (!listNumTags.isEmpty()) {
			stdDev = sumDeviations / listNumTags.size();
			logger.info("### Standard deviation of number of tags per photo: " + sumDeviations / listNumTags.size());
		}
		saveTagsFromInterestingPhotos(date, stats, nbPhotosProcessed, sumTags, maxTags, stdDev);
	}

	/**
	 * Save the list of tags into a file, including tag id, raw value and count of explored photos that have that tag.
	 * 
	 * @param date
	 * @param stats list of tags retrieved
	 * @param nbPhotosProcessed Number of photos explored during the whole process
	 * @param sumTags Sum of the number of tags each photo has (to be able to compute the average number of tags per photo)
	 * @param maxGroups Max number of groups each photo belongs to
	 * @param stdDeviation Standard deviation of number of groups a photo belongs to
	 * @throws IOException
	 */
	private static void saveTagsFromInterestingPhotos(String date, HashMap<String, TagItemStat> stats, int nbPhotosProcessed, int sumTags, int maxTags, float stdDeviation) throws IOException {

		File file = new File(Util.getDir(config.getString("fm.flickr.stat.tag.dir")), date + ".log");
		FileOutputStream fos = new FileOutputStream(file);
		PrintWriter writer = new PrintWriter(fos);

		logger.info("Saving " + stats.size() + " tags into file " + file.getCanonicalPath());

		writer.println("# Processed " + stats.size() + " tags from " + nbPhotosProcessed + " photos");
		writer.println("# Number of photos explored: " + nbPhotosProcessed);
		writer.println("# Total number of tags: " + sumTags);
		writer.println("#");
		writer.println("# Average number of tags per photo: " + (new Float(sumTags)) / nbPhotosProcessed);
		writer.println("# Maximum number of tags per photo: " + maxTags);
		writer.println("# Standard deviation of the number of tags per photo: " + stdDeviation);
		writer.println("#");
		writer.println("# tagId SEPARATOR raw value of the tag SEPARATOR number of occurence of the tag");
		writer.println("#");

		Collection<TagItemStat> grpset = stats.values();
		Iterator<TagItemStat> iter = grpset.iterator();
		while (iter.hasNext()) {
			TagItemStat entry = iter.next();
			writer.println(entry.toFile());
		}
		writer.close();
		fos.close();
	}

	/**
	 * Load the content of the file for the given date, into the map of statistics
	 * 
	 * @param date date of data collected from Interestingness, given in format "YYY-MM-DD"
	 */
	public static void loadFileByDay(String date) throws ServiceException {

		String fileName = config.getString("fm.flickr.stat.tag.dir") + date + ".log";
		loadFile(new File(fileName));
		logger.info("### " + statistics.size() + " total tags loaded.");
	}

	/**
	 * Load the content of the all files for the given month, into the map of statistics
	 * 
	 * @param yearMonth year and month formatted as yyyy-mm
	 */
	public static void loadFilesByMonth(String yearMonth) throws ServiceException {
		// Empty the current data if any
		statistics.clear();
		statisticsTpP.clear();

		File dir = new File(config.getString("fm.flickr.stat.tag.dir"));
		if (!dir.exists() || !dir.isDirectory()) {
			String errMsg = "Error: data directory " + config.getString("fm.flickr.stat.tag.dir") + " does not exists.";
			logger.warn(errMsg);
			throw new ServiceException(errMsg);
		}

		for (File file : dir.listFiles())
			if (file.getName().startsWith(yearMonth))
				loadFile(file);

		logger.info("### " + statistics.size() + " total tags loaded for period " + yearMonth);
	}

	/** 
	 * Parse the content of the given file and store its content into the static map statistics
	 * @param file  
	 */
	private static void loadFile(File file) throws ServiceException {
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
				if (str.trim().startsWith("#")) {
					// Process comment lines like:
					// # Average number of tags per photo: 9
					// # Maximum number of tags per photo: 54
					// # Standard deviation of the number of tags per photo: 3
					TagsPerPhoto gpp = new TagsPerPhoto();

					String strSeeked = "# Average number of tags per photo: ";
					if (str.startsWith(strSeeked)) {
						gpp.setAvgTagsPerPhoto(Float.valueOf(str.substring(strSeeked.length())));

						str = buffer.readLine();
						strSeeked = "# Maximum number of tags per photo: ";
						if (str.startsWith(strSeeked))
							gpp.setMaxTagsPerPhoto(Integer.valueOf(str.substring(strSeeked.length())));

						str = buffer.readLine();
						strSeeked = "# Standard deviation of the number of tags per photo: ";
						if (str.startsWith(strSeeked))
							gpp.setStdDevTagsPerPhoto(Float.valueOf(str.substring(strSeeked.length())));

						statisticsTpP.add(gpp);
					}

				} else {
					String[] tokens = str.split(TagItemStat.FIELD_SEPARATOR);
					if (tokens.length != 3)
						logger.warn("Wrong format on line: " + str);
					else {
						String tagId = tokens[0];

						TagItemStat tagStat = statistics.get(tagId);
						if (tagStat != null) {
							// Either increment the number of occurences if the tag already exists
							tagStat.incNbOccurences(Integer.valueOf(tokens[2]));
							logger.trace("Updating tag " + tagId + ": " + tagStat.getNbOccurences());

						} else {
							// Or create a new tag element
							TagItemStat tag = new TagItemStat(tokens[0], tokens[1], Integer.valueOf(tokens[2]));
							logger.trace("Adding tag " + tagId);
							statistics.put(tagId, tag);
						}
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
	 * Print the header line following the csv format
	 * @param ps
	 * @throws FileNotFoundException
	 */
	public static void initComputeMonthly(PrintStream ps) throws FileNotFoundException {
		ps.print("#month; ");
		ps.println("avg tags/photo; std dev tags/user; max tags/user");
	}

	/**
	 * Filter only tags with at least a minimum number of occurences (score) and sort by score
	 * @param ps where to print the output
	 */
	public static void computeStatistics(PrintStream ps) {
		logger.info("Computing statistincs of tags");
		Collection<TagItemStat> tagset = statistics.values();
		ArrayList<TagItemStat> tagList = new ArrayList<TagItemStat>(tagset);
		Collections.sort(tagList);

		for (int i = 0; i < tagList.size(); i++) {
			TagItemStat entry = tagList.get(i);

			// Filter only tags with a minimum number of occurences
			if (entry.getNbOccurences() >= config.getInt("fm.flickr.stat.tag.minoccurence")) {
				ps.println((i + 1) + ": " + entry.toStringL());
			}
		}
		ps.println();
	}

	/**
	 * Display the average/std deviation and maximum number of groups and photos
	 * The average is calculated as the average of daily average values, which is incorrect mathematically.
	 * But in this specific case it works as the average is done on the same number of data every day, 
	 * that is 500 explorted photos. 
	 * 
	 * @param ps where to print the output
	 * @param month in case of processing data by month, this string denotes the current month formatted as yyyy-mm. May be null.
	 */
	public static void computeMonthlyStatistics(PrintStream ps, String month) {
		float sumAvg = 0;
		float sumStdDev = 0;
		int sumMax = 0;
		for (TagsPerPhoto gpp : statisticsTpP) {
			sumAvg += gpp.getAvgTagsPerPhoto();
			sumStdDev += gpp.getStdDevTagsPerPhoto();
			sumMax += gpp.getMaxTagsPerPhoto();
		}

		ps.print(month + "; ");
		ps.printf("%2.4f; ", sumAvg / statisticsTpP.size());
		ps.printf("%2.4f; ", sumStdDev / statisticsTpP.size());
		ps.println(sumMax / statisticsTpP.size());
	}
}
