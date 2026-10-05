package nl.errorsoft.esql.blob.ui;

/** The window of an upload or download, as the controller sees it. */
public interface UDDataIF {
	/** Shows the window and waits until it is closed. */
	void open();

	/** Called on the event thread with the percentage done, the window closes at 100. */
	void setProgressValue(int percentage);

	/** Called on the event thread when the transfer failed, so that it can be started again. */
	void transferEnded();
}
