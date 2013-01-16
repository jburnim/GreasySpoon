package icap.services.resources.gs;

/**
 * Custom exception used for scripts execution timeout
 */
public class SpoonScriptException extends Exception {
	
	private static final long serialVersionUID = 1L;

	/**
	 * @param message
	 */
	public SpoonScriptException(String message) {
		super(message);
	}
		
	public synchronized Throwable fillInStackTrace() {
		// do nothing=> performance optimization as stack trace is of no use here
		return this;
	}
}
