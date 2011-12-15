package fm.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.log4j.Logger;

/**
 * <p>Config is a singleton class that encapsculates the Apache commons-configuration.</p>
 * <p>The initConfiguration() methods create a composite configuration by loading the files listed in
 * the default composite configuration file /config.xml, or in a specific file provided by caller.</p>
 * <p>The composite configuration file can be given as a complete path or relative path, in the latter
 * it should be found in based on the classpath.</p>
 * @author fmichel
 */
public class Config
{
	/** Default config file name */
	private static String DEFAULT_CONFIG_FILENAME = "/config.xml";

	private static Logger logger = Logger.getLogger(Config.class);

	private static org.apache.commons.configuration.Configuration config = null;

	/**
	 * Returns user home directory as defined in "HOME" environment variable. If "HOME" is not defined,
	 * "USERPROFILE" environment variable is used. If neither variable is defined, return null.
	 * @return user home directory or null if the environment is not properly set
	 */
	public static String getHome() {
		String h = System.getenv("HOME");
		if (h == null) {
			return System.getenv("USERPROFILE");
		} else
			return h;
	}

	/**
	 * Return client configuration after initialization (if needed). 
	 * @return configuration
	 */
	public static synchronized org.apache.commons.configuration.Configuration getConfiguration() {
		if (config == null) {
			config = initConfiguration();
		}
		return config;
	}

	/**
	 * Creates a composite configuration based on the default project configuration file
	 */
	protected static synchronized org.apache.commons.configuration.Configuration initConfiguration() {

		org.apache.commons.configuration.Configuration config = null;
		DefaultConfigurationBuilder factory = new DefaultConfigurationBuilder();
		factory.setURL(Config.class.getResource(DEFAULT_CONFIG_FILENAME));
		try {
			config = factory.getConfiguration();
		} catch (ConfigurationException e) {
			e.printStackTrace();
			logger.error(e);
		}
		return config;
	}
}
