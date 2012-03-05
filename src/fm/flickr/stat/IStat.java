package fm.flickr.stat;

import java.io.IOException;
import java.io.PrintStream;

import fm.flickr.api.wrapper.service.param.PhotoItemsSet;
import fm.flickr.api.wrapper.util.ServiceException;

/**
 * This interface must be implemented by any class whishing to collect data from Interestingness, besides basic 
 * per-photo data already retrieved by the interestingness.getList service, and provide a way to perform statistic 
 * analysis over these data.
 * 
 * @author fmichel
 */
public interface IStat
{
	/**
	 * <p>Based on the list of photos retrieved from Interestingness, perform any extra action needed to
	 * store data that shall later be used to perform statistics.</p>
	 * <p>The results are saved into files by the implementing class.</p>
	 * 
	 * @param date date of photos from Interestingness, given in format "YYY-MM-DD"
	 * @param photoSet photos retrieved from Interestingness
	 */
	public void collecAdditionalData(String date, PhotoItemsSet photos) throws IOException;

	/**
	 * Load data of a given day into memory, to further compute statistics.
	 * @param date date of data collected from Interestingness, given in format "YYY-MM-DD"
	 */
	public void loadFileByDay(String date) throws ServiceException;

	/**
	 * Load all data files for a given month into memory, to further compute statistics.
	 * Any file name strating with "yyyy-mm" will be loaded, may there be 1 or 31 files for that month.
	 * @param yearMonth year and month formatted as yyyy-mm
	 */
	public void loadFilesByMonth(String yearMonth) throws ServiceException;

	/**
	 * Computation of data loaded from files to produce special statistics
	 * @param ps where to print the output
	 */
	public void computeStatistics(PrintStream ps);
}
