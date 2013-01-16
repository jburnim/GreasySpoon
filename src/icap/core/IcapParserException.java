package icap.core;

/**
 * Used to indicate error while parsing ICAP request (either in REQ or RESP mode)
 * @author greasyspoon
 */
public class IcapParserException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7608072256267927898L;

	/**
	 * Create a new IcapParserException
	 * @param message associated to the exception
	 */
	public IcapParserException(String message){
		super(message);
	}
}
