package nl.errorsoft.esql.blob;

import java.io.InputStream;
import java.util.function.IntConsumer;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.jdbc.DatabaseConnection;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.TableData;
import nl.errorsoft.esql.table.TableService;

/** Uploads a file into a binary cell and saves a binary cell to a file, reports progress (0 to 100) to the progress callback. */
public class BlobService {
	private static final Logger log = LogManager.getLogger(BlobService.class);

	private IntConsumer progress = percent -> {
	};
	private final BlobRepository repository;
	private final TableService tables;

	public BlobService(BlobRepository repository, TableService tables) {
		this.repository = repository;
		this.tables = tables;
	}

	public void upload(Table table, TableData[] row, TableData cell, String file) throws Exception {
		String condition = tables.rowFilter(row);
		Path source = Path.of(file);
		long size = Files.size(source);
		log.debug("Uploading {} bytes from {}", size, file);

		try (InputStream content = Files.newInputStream(source)) {
			repository.write(table, cell.getTableColumn().getName(), condition, content, size);
		}
		progress(100);
	}

	public void download(Table table, TableData[] row, TableData cell, String file) throws Exception {
		String condition = tables.rowFilter(row);

		try (OutputStream target = Files.newOutputStream(Path.of(file))) {
			if (!repository.read(table, cell.getTableColumn().getName(), condition, target)) {
				log.warn("No row found to save to {}", file);
			}
		}

		progress(100);
	}

	/** Called with the percentage done, from the thread that runs the transfer. */
	public void setProgress(IntConsumer progress) {
		this.progress = progress;
	}

	private void progress(int percent) {
		progress.accept(percent);
	}
}
