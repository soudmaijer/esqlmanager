package nl.errorsoft.esql.job;

import java.util.List;

/** Told how far a long running job is. Called from the thread the job runs on, not the event thread. */
public interface ProgressListener {
	/** Ignores everything, for a job nobody listens to. */
	ProgressListener NONE = new ProgressListener() {
		@Override
		public void progressed(int percent) {
		}

		@Override
		public void failed(Exception error) {
		}
	};

	/** @param percent 0 to 100 */
	void progressed(int percent);

	/** The job stopped with an error. */
	void failed(Exception error);

	/** What the job is doing or how far it got in words ("42 statements done"). */
	default void status(String text) {
	}

	/**
	 * The job ran to the end.
	 * @param summary what it did ("Ran 120 statements")
	 * @param details lines to show with the summary, such as the statements that failed; empty when there is nothing to add
	 */
	default void finished(String summary, List<String> details) {
	}

	/** The job stopped because the user cancelled it. @param summary how far it got and what was left behind */
	default void cancelled(String summary) {
	}
}
