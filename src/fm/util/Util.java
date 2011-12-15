package fm.util;

import java.io.File;

public class Util
{
	/**
	 * Return a file instance of the directory denoted by dirName.
	 * If it does not exist, dirName and all its parent dirs are created
	 * @param dirName
	 * @return
	 */
	public static File getDir(String dirName) {
		File dir = new File(dirName);
		if (dir.exists() && dir.isDirectory())
			return dir;

		dir.mkdirs();
		return dir;
	}
}
