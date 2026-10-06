package nl.errorsoft.esql.driver.control;

import java.awt.Component;
import java.util.HashSet;
import java.util.Set;
import javax.swing.SwingUtilities;

import nl.errorsoft.esql.app.ApplicationContext;
import nl.errorsoft.esql.driver.DriverArtifact;
import nl.errorsoft.esql.driver.DriverService;
import nl.errorsoft.esql.driver.DriverSource;
import nl.errorsoft.esql.driver.ui.dialog.DriverDownloadDialog;
import nl.errorsoft.esql.job.ProgressListener;
import nl.errorsoft.esql.ui.dialog.Dialogs;

/** Downloads a JDBC driver that is not bundled, with a progress dialog, when the user asks for it or agrees to it before connecting. */
public class DriverDownloadController {
	/** The jars being downloaded now, by file name; only touched on the event thread. */
	private static final Set<String> DOWNLOADING = new HashSet<>();

	private final Component parent;

	/** @param parent the window the questions, the progress and the errors belong to */
	public DriverDownloadController(Component parent) {
		this.parent = parent;
	}

	/**
	 * Runs {@code then} when the driver can be used. When it has to be downloaded first, asks the user, downloads it and runs {@code then} afterwards;
	 * {@code otherwise} runs when the user declines or the download fails (which is reported). Event thread only, both callbacks run on it.
	 */
	public void ensureDriver(DriverSource source, Runnable then, Runnable otherwise) {
		DriverService drivers = ApplicationContext.get().drivers();
		if (!drivers.needsDownload(source)) {
			then.run();
			return;
		}
		DriverArtifact artifact = source.artifact();
		String question = "Download " + artifact.name() + " driver (" + artifact.sizeText() + ", licence " + artifact.licence() + ")?\n\n"
			+ "The driver is not included with eSQLManager because of its licence. It is downloaded once from Maven Central into "
			+ drivers.jarOf(artifact).getParent() + ".";
		if (Dialogs.confirm(parent, "Download " + artifact.name() + " driver", question, "Download")) {
			download(artifact, then, otherwise);
		} else {
			otherwise.run();
		}
	}

	/** Downloads the driver on a virtual thread with a progress dialog; {@code done} or {@code failed} runs on the event thread. */
	public void download(DriverArtifact artifact, Runnable done, Runnable failed) {
		// The connect dialog and the driver settings can both ask for it: one download of a jar at a time.
		if (!DOWNLOADING.add(artifact.fileName())) {
			Dialogs.info(parent, "Download " + artifact.name() + " driver", "The " + artifact.name() + " driver is being downloaded already.");
			failed.run();
			return;
		}
		DriverDownloadDialog progress = new DriverDownloadDialog(Dialogs.windowOf(parent), artifact);
		progress.setVisible(true);
		ProgressListener listener = new ProgressListener() {
			@Override
			public void progressed(int percent) {
				SwingUtilities.invokeLater(() -> progress.progressed(percent));
			}

			@Override
			public void failed(Exception error) {
				SwingUtilities.invokeLater(() -> {
					DOWNLOADING.remove(artifact.fileName());
					progress.dispose();
					ApplicationContext.get().errors().report(parent, "Download " + artifact.name() + " driver", error);
					failed.run();
				});
			}
		};
		Thread.ofVirtual().name("driver-download").start(() -> {
			try {
				ApplicationContext.get().drivers().download(artifact, listener);
				SwingUtilities.invokeLater(() -> {
					DOWNLOADING.remove(artifact.fileName());
					progress.dispose();
					done.run();
				});
			} catch (Exception e) {
				listener.failed(e);
			}
		});
	}
}
