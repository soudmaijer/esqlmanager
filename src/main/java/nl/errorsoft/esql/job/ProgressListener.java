package nl.errorsoft.esql.job;

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

	void failed(Exception error);
}
