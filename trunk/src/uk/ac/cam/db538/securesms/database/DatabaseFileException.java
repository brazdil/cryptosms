package uk.ac.cam.db538.securesms.database;

/**
 * 
 * Exception thrown by the Database class object
 * 
 * @author David Brazdil
 *
 */
public class DatabaseFileException extends Exception {
	private static final long serialVersionUID = -7100685462486843982L;

	public DatabaseFileException(String message) {
		super(message);
	}
}
