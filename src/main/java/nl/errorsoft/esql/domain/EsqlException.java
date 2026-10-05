package nl.errorsoft.esql.domain;

/**
 * A problem the user can understand and fix, such as a row that can't be identified or a feature the database does not have.
 * The message is shown as it is. Any other exception is unexpected and is logged with its stack trace.
 */
public class EsqlException extends RuntimeException {
	public EsqlException(String message) {
		super(message);
	}

	public EsqlException(String message, Throwable cause) {
		super(message, cause);
	}
}
