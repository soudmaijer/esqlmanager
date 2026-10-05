package nl.errorsoft.esql.settings;

import nl.errorsoft.esql.table.Table;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import org.jdom.*;
import org.jdom.input.SAXBuilder;

public class Settings {
	private static final Logger log = LogManager.getLogger(Settings.class);

	private Appearance appearance = Appearance.SYSTEM;

	// Table types & field types
	private String[] tabletypes = new String[0];

	public Settings() {
		loadSettings();
	}

	public Appearance getAppearance() {
		return appearance;
	}

	public void setAppearance(Appearance appearance) {
		this.appearance = appearance;
	}

	public void loadSettings() {
		try {
			SAXBuilder builder = new SAXBuilder();
			org.jdom.Document sdata = builder.build(new File("conf/settings.xml"));

			if (sdata.hasRootElement()) {
				appearance = Appearance.of(sdata.getRootElement().getChildText("appearance"));
			}

			sdata = null;
			builder = null;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public boolean saveSettings() {
		String set = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + '\n';
		set = set + "<config>" + '\n';
		set = set + "	<appearance>" + appearance.name() + "</appearance>" + '\n';
		set = set + "</config>" + '\n';

		try {
			try (PrintWriter out = new PrintWriter(new FileWriter("conf/settings.xml"))) {
				out.println(set);
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
