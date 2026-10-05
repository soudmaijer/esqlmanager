package nl.errorsoft.esql.app.control;

import java.io.Serializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import nl.errorsoft.esql.app.ui.ESQLManagerUI;

/**
 * Shows the application log in the output panel at the bottom of the main window.
 */
public class OutputPanelAppender extends AbstractAppender {
	private final ESQLManagerUI ui;

	private OutputPanelAppender(Layout<? extends Serializable> layout, ESQLManagerUI ui) {
		super("OutputPanel", null, layout, true, Property.EMPTY_ARRAY);
		this.ui = ui;
	}

	public void append(LogEvent event) {
		ui.print(new String(getLayout().toByteArray(event)));
	}

	/** Attaches the output panel to the root logger. */
	public static void install(ESQLManagerUI ui) {
		LoggerContext context = (LoggerContext) LogManager.getContext(false);
		Configuration config = context.getConfiguration();
		Layout<? extends Serializable> layout = PatternLayout.newBuilder().setPattern("%d{HH:mm:ss} %-5level %msg%n").setConfiguration(config).build();
		Appender appender = new OutputPanelAppender(layout, ui);

		appender.start();
		config.addAppender(appender);
		config.getRootLogger().addAppender(appender, null, null);
		context.updateLoggers();
	}
}
