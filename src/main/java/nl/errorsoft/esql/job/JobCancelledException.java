package nl.errorsoft.esql.job;

/** Thrown inside a job when the user cancelled it, to leave the work at the next place where it is safe to stop. It is not an error. */
public class JobCancelledException extends RuntimeException {
	public JobCancelledException() {
		super("Cancelled");
	}
}
