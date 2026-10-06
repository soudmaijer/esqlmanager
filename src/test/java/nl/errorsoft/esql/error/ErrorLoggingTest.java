package nl.errorsoft.esql.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.errorsoft.esql.app.OutputPanelAppender;

/** The output panel shows one line for an unexpected error, the log file has the stack trace. */
class ErrorLoggingTest {
	@TempDir
	Path folder;

	private final List<String> panel = new ArrayList<>();
	private LoggerContext context;
	private LoggerConfig logger;
	private AbstractAppender panelAppender;
	private FileAppender fileAppender;

	@BeforeEach
	void attach() {
		context = (LoggerContext) LogManager.getContext(false);
		Configuration config = context.getConfiguration();
		panelAppender = new AbstractAppender("TestPanel", null, OutputPanelAppender.layout(config), true, Property.EMPTY_ARRAY) {
			@Override
			public void append(LogEvent event) {
				panel.add(new String(getLayout().toByteArray(event), StandardCharsets.UTF_8));
			}
		};
		// The layout of the configured log file, writing to a temporary folder.
		fileAppender = FileAppender.newBuilder()
			.setName("TestFile")
			.withFileName(folder.resolve("esqlmanager.log").toString())
			.setLayout(config.getAppender("File").getLayout())
			.setConfiguration(config)
			.build();
		panelAppender.start();
		fileAppender.start();
		logger = config.getLoggerConfig(ErrorHandler.class.getName());
		logger.addAppender(panelAppender, null, null);
		logger.addAppender(fileAppender, null, null);
		context.updateLoggers();
	}

	@AfterEach
	void detach() {
		logger.removeAppender("TestPanel");
		logger.removeAppender("TestFile");
		context.updateLoggers();
		panelAppender.stop();
		fileAppender.stop();
	}

	@Test
	void anUnexpectedErrorIsOneLineInThePanelAndAStackTraceInTheFile() throws IOException {
		ErrorHandler.log("Unexpected error", new IllegalStateException("boom"));
		fileAppender.stop();

		assertEquals(1, panel.size());
		String line = panel.getFirst();
		assertTrue(line.contains("Unexpected error: boom (details in esqlmanager.log)"), line);
		assertEquals(1, line.lines().count(), line);
		assertFalse(line.contains("IllegalStateException"), line);

		String file = Files.readString(folder.resolve("esqlmanager.log"));
		assertTrue(file.contains("java.lang.IllegalStateException: boom"), file);
		assertTrue(file.contains("at nl.errorsoft.esql.error.ErrorLoggingTest"), file);
	}

	@Test
	void aProblemTheUserCanFixKeepsItsMessage() {
		ErrorHandler.log("Drop table", new EsqlException("Select a table first."));

		assertEquals(1, panel.size());
		assertTrue(panel.getFirst().contains("Drop table: Select a table first."), panel.getFirst());
	}
}
