package nl.errorsoft.esql.app;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import nl.errorsoft.esql.app.ui.MainWindow;

/**
 * Shows the application log in the output panel at the bottom of the main window.
 */
public class OutputPanelAppender extends AbstractAppender {
	private final MainWindow mainWindow;

	private OutputPanelAppender(Layout<? extends Serializable> layout, MainWindow mainWindow) {
		super("OutputPanel", null, layout, true, Property.EMPTY_ARRAY);
		this.mainWindow = mainWindow;
	}

	public void append(LogEvent event) {
		mainWindow.print(new String(getLayout().toByteArray(event), StandardCharsets.UTF_8));
	}

	/** Attaches the output panel to the root logger. */
	public static void install(MainWindow mainWindow) {
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		Configuration config = context.getConfiguration();
		Layout<? extends Serializable> layout = PatternLayout.newBuilder().setPattern("%d{HH:mm:ss} %-5level %msg%n").setCharset(StandardCharsets.UTF_8)
			.setConfiguration(config).build();
		Appender appender = new OutputPanelAppender(layout, mainWindow);

		appender.start();
		config.addAppender(appender);
		config.getRootLogger().addAppender(appender, null, null);
		context.updateLoggers();
	}
}
