package nl.errorsoft.esql.blob;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.IntConsumer;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.job.Cancellation;
import nl.errorsoft.esql.job.JobCancelledException;
import nl.errorsoft.esql.error.EsqlException;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableCell;
import nl.errorsoft.esql.table.TableService;

/** Uploads a file into a binary cell and saves a binary cell to a file, reports progress (0 to 100) to the progress callback. */
public class BlobService {
	private static final Logger log = LogManager.getLogger(BlobService.class);

	private final Cancellation cancellation = new Cancellation();
	private IntConsumer progress = percent -> {
	};
	private final BlobRepository repository;
	private final TableService tables;

	public BlobService(BlobRepository repository, TableService tables) {
		this.repository = repository;
		this.tables = tables;
	}

	public void upload(Table table, TableCell[] row, TableCell cell, String file) throws Exception {
		String condition = tables.rowFilter(row);
		Path source = Path.of(file);
		long size = Files.size(source);
		log.debug("Uploading {} bytes from {}", size, file);

		try (InputStream content = new TransferInputStream(Files.newInputStream(source), size)) {
			repository.write(table, cell.getTableColumn().getName(), condition, content, size);
		}
		progress(100);
	}

	public void download(Table table, TableCell[] row, TableCell cell, String file) throws Exception {
		String condition = tables.rowFilter(row);

		try (OutputStream target = new CancellableOutputStream(Files.newOutputStream(Path.of(file)))) {
			if (!repository.read(table, cell.getTableColumn().getName(), condition, target)) {
				throw new EsqlException("No row found for " + table + " to save " + cell.getTableColumn().getName() + " from.");
			}
		}

		progress(100);
	}

	/** Asks the transfer to stop. Safe to call from any thread; the transfer then fails with {@link JobCancelledException} or an error of the driver. */
	public void cancel() {
		cancellation.cancel();
	}

	public boolean isCancelled() {
		return cancellation.isCancelled();
	}

	/** Called with the percentage done, from the thread that runs the transfer. */
	public void setProgress(IntConsumer progress) {
		this.progress = progress;
	}

	private void progress(int percent) {
		progress.accept(percent);
	}

	/** Checks for a cancel at every read and reports the percentage read (never 100, the transfer is done when the statement returns). */
	private final class TransferInputStream extends FilterInputStream {
		private final long size;
		private long read;
		private int reported = -1;

		TransferInputStream(InputStream in, long size) {
			super(in);
			this.size = Math.max(1, size);
		}

		@Override
		public int read() throws IOException {
			cancellation.check();
			int b = super.read();
			advance(b < 0 ? 0 : 1);
			return b;
		}

		@Override
		public int read(byte[] buffer, int offset, int length) throws IOException {
			cancellation.check();
			int n = super.read(buffer, offset, length);
			advance(Math.max(n, 0));
			return n;
		}

		private void advance(int bytes) {
			read += bytes;
			int percent = (int) Math.min(99, 100 * read / size);
			if (percent != reported) {
				reported = percent;
				progress(percent);
			}
		}
	}

	/** Checks for a cancel at every write. */
	private final class CancellableOutputStream extends FilterOutputStream {
		CancellableOutputStream(OutputStream out) {
			super(out);
		}

		@Override
		public void write(int b) throws IOException {
			cancellation.check();
			out.write(b);
		}

		@Override
		public void write(byte[] buffer, int offset, int length) throws IOException {
			cancellation.check();
			out.write(buffer, offset, length);
		}
	}
}
