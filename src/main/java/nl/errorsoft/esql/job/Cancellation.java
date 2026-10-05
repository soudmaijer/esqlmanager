package nl.errorsoft.esql.job;

/** The cancel flag of one job: the user sets it from the event thread, the job checks it between statements and rows. */
public final class Cancellation {
	private volatile boolean cancelled;

	public void cancel() {
		cancelled = true;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	/** @throws JobCancelledException when the job was cancelled. */
	public void check() {
		if (cancelled) {
			throw new JobCancelledException();
		}
	}
}
