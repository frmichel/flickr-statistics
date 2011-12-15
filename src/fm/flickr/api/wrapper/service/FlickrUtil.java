package fm.flickr.api.wrapper.service;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import fm.flickr.api.wrapper.util.ServiceException;
import fm.util.Config;

/**
 * Provide util functions to handle the Flickr API: build a request, sign it, format parametesr, launch the request.
 * 
 * @author fmichel
 *
 */
public class FlickrUtil
{
	private static Logger logger = Logger.getLogger(FlickrUtil.class.getName());

	private static Configuration config = Config.getConfiguration();

	/**
	 * Launch an http GET request to a flickr service. Fill try 3 times in case of IO error, and will
	 * then throw an exception after the 3rd error.
	 * 
	 * @param urlStr service url incliding parameters
	 * @return payload xml repsonse from the server
	 * @throws ServiceException in case any error occurs: http connection error, flickr error,
	 * response parsing error
	 */
	public static Document launchRequest(String urlStr) throws ServiceException {
		int attempts = 0;
		while (attempts < config.getInt("fm.flickr.api.wrapper.nb_tries")) {
			try {
				URL url = new URL(urlStr);
				logger.debug("Will get url: " + url.toString());
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				con.setConnectTimeout(config.getInt("fm.flickr.api.wrapper.connection_timeout"));
				con.setReadTimeout(config.getInt("fm.flickr.api.wrapper.read_timeout"));

				// Run request and check HTTP status code
				con.connect();
				if (con.getResponseCode() != 200) {
					logger.error("Request failed, HTTP response: " + con.getResponseMessage());
					throw new ServiceException("Request failed, HTTP response: " + con.getResponseMessage());
				}

				// Read and parse the XML response
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document xmlResp = db.parse(con.getInputStream());
				logger.trace("XML payload response: " + xmlToString(xmlResp));

				// Check the Flickr status
				String error = FlickrUtil.checkFlickrResponseStatus(xmlResp);
				if (error != null) {
					logger.warn("Flickr returned an error: " + error);
					throw new ServiceException("Flickr returned an error: " + error);
				} else
					return xmlResp;

			} catch (IOException e) {
				logger.error("Connection failed", e);
				attempts++;
				if (attempts >= config.getInt("fm.flickr.api.wrapper.nb_tries"))
					throw new ServiceException("IO error, http connection failed");
				else
					logger.error("Trying attempt #" + (attempts + 1));
			} catch (ParserConfigurationException e) {
				logger.error( "Invalid XML response", e);
				throw new ServiceException("Invalid XML response");
			} catch (SAXException e) {
				logger.error( "SAX error while parsing repsonse", e);
				throw new ServiceException("SAX error while parsing response");
			}
		} // end while nb attempts

		logger.error( "We should never executre this code!");
		return null;
	}

	/**
	 * Check the status of the flickr response. A response with status ok will look like this:
	 * 
	 * <pre>
	 * &lt;rsp stat=&quot;ok&quot;&gt;
	 *    [xml-payload-here]
	 * &lt;/rsp&gt;
	 * </pre>
	 * 
	 * while an error response will look like this:
	 * 
	 * <pre>
	 * &lt;rsp stat=&quot;fail&quot;&gt; 
	 *   &lt;err code=&quot;100&quot; msg=&quot;Invalid API Key (Key not found)&quot; /&gt; 
	 * &lt;/rsp&gt;
	 * </pre>
	 * 
	 * @param XML document representing the payload response received from Flickr
	 * @return null if the status is ok, otherwise a string with the error code and cause.
	 */
	public static String checkFlickrResponseStatus(Document xmlResponse) {

		Node respNode = xmlResponse.getElementsByTagName("rsp").item(0);
		String status = ((Element) respNode).getAttribute("stat");

		if (!"ok".equalsIgnoreCase(status)) {
			Node errNode = ((Element) respNode).getElementsByTagName("err").item(0);
			String code = ((Element) errNode).getAttribute("code");
			String msg = ((Element) errNode).getAttribute("msg");
			return "Code: " + code + ", Cause: " + msg; //$NON-NLS-2$
		}
		return null;
	}

	/**
	 * Calculate the signature of the query based on the list of parameters and values.
	 * <p>
	 * The signature is the MD5 hash of the string composed as the concatenation of the shared
	 * secret and each parameter name and value, sorted in ascending order of the parameters names.
	 * </p>
	 * <p>
	 * Ultimately, the signature is inserted into the list of parameters with name <i>api_sig</i>,
	 * and the list of parameters is returned by concatenating names and values in a URL format,
	 * that is:
	 * 
	 * <pre>
	 * &amp;param1=value1&amp;param2=value2
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @see http://www.flickr.com/services/api/auth.spec.html section 8
	 * @param secret the shared secret of the application
	 * @param listParams parameters in ascending sorted order
	 * @return formated list if parameters ready to add in a URL
	 */
	public static String signApi(String secret, TreeMap<String, String> listParams) {

		StringBuilder signString = new StringBuilder();
		signString.append(secret);

		// Concat name and value of each parameter in param ascending order
		for (String paramName : listParams.keySet()) {
			signString.append(paramName);
			signString.append(listParams.get(paramName));
		}

		// Calculate the MD5 hash
		try {
			logger.trace("String to hash: " + signString);
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(signString.toString().getBytes());
			byte[] signature = md.digest();

			// Add the signature to the list of parameters
			listParams.put("api_sig", byteToHexString(signature));
			return formatUrlParams(listParams);

		} catch (NoSuchAlgorithmException e) {
			logger.error( "Error getting MessageDigest", e);
			return null;
		}
	}

	/**
	 * Build the parameters part of the URL of Flickr request by concatenating names and values,
	 * that is:
	 * 
	 * <pre>
	 * &amp;param1=value1&amp;param2=value2
	 * </pre>
	 * 
	 * @param listParams
	 * @return formated list of params and vlaues
	 */
	public static String formatUrlParams(TreeMap<String, String> listParams) {
		StringBuilder params = new StringBuilder();

		boolean first = true;
		for (String key : listParams.keySet()) {
			if (first)
				first = false;
			else
				params.append("&");
			try {
				params.append(key + "=" + URLEncoder.encode(listParams.get(key), "UTF-8")); //$NON-NLS-2$
			} catch (UnsupportedEncodingException e) {
				//GWT.log("Encoding error", e);
			}
			//params.append(key + "=" + listParams.get(key));

		}
		return params.toString();
	}

	/**
	 * Turn an array of bytes into its string reprensentation in hexadecimal base
	 * 
	 * @param buf Array of bytes to convert into hexadecimal reprensetation
	 * @return Generated hexadecimal reprensetation
	 */
	private static String byteToHexString(byte buf[]) {
		StringBuffer strbuf = new StringBuffer(buf.length * 2);
		int i;

		for (i = 0; i < buf.length; i++) {
			if (((int) buf[i] & 0xff) < 0x10) {
				strbuf.append("0");
			}
			strbuf.append(Long.toString((int) buf[i] & 0xff, 16));
		}
		return strbuf.toString();
	}

	/**
	 * Make a string representation of an XML node or document
	 * 
	 * @param node
	 * @return
	 */
	public static String xmlToString(Node node) {
		try {
			StringWriter stringWriter = new StringWriter();
			Result result = new StreamResult(stringWriter);

			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.transform(new DOMSource(node), result);
			return stringWriter.getBuffer().toString();

		} catch (TransformerConfigurationException e) {
			logger.error( "Exception catched", e);
		} catch (TransformerException e) {
			logger.error( "Exception catched", e);
		}
		return null;
	}
}
