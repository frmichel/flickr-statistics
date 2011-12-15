package fm.flickr.api.wrapper.util;

/**
 * @author fmichel
 */
public class ServiceException extends Exception
{
	private static final long serialVersionUID = 2373702548908057894L;

	public ServiceException(String message) {
		super(message);
	}

	public ServiceException(Throwable cause) {
		super(cause);
	}

	public ServiceException(String message, Throwable cause) {
		super(message, cause);
	}

}
