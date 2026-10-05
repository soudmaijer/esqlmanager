package nl.errorsoft.esql.connection;

import nl.errorsoft.esql.app.DataDirectory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class DatabaseDriver {
	private static final Logger log = LogManager.getLogger(DatabaseDriver.class);

	private int id;
	private String driverName;
	private String driverURL;
	private String driverClassName;
	private String fieldOpenChar;
	private String fieldCloseChar;
	private String dataOpenChar;
	private String dataCloseChar;
	private DatabaseDriver[] drivers;
	private org.jdom.Document driverData;

	/*
	 * @author:			S.Oudmaijer
	 * @description:	Reads the XML data from the driver.xml file in de application root.
	 */
	public DatabaseDriver() {
		this(true);
	}

	/** One entry of driver.xml ({@code readFile} false), or the reader of the whole file. */
	private DatabaseDriver(boolean readFile) {
		drivers = new DatabaseDriver[0];
		if (!readFile) {
			return;
		}

		try {
			SAXBuilder saxbuilder = new SAXBuilder();
			driverData = saxbuilder.build(DataDirectory.file("conf/driver.xml"));
		} catch (Exception exception) {
			log.warn("Warning: driver.xml could not be loaded, no driver properties will be available!");
		}
	}

	/*
	 * @author:			S.Oudmaijer
	 * @description:	Retreives all database drivers from the JDOM document.
	 */
	public DatabaseDriver[] getDatabaseDrivers() {
		if (driverData == null || !driverData.hasRootElement()) {
			return drivers;
		}

		List<DatabaseDriver> read = new java.util.ArrayList<>();

		for (Object entry : driverData.getRootElement().getChildren("driver")) {
			Element element = (Element) entry;

			try {
				DatabaseDriver driver = new DatabaseDriver(false);
				driver.setId(Integer.parseInt(element.getChildTextTrim("id")));
				driver.setDriverName(required(element, "driverName"));
				driver.setDriverURL(required(element, "driverURL"));
				driver.setDriverClassName(required(element, "driverClassName"));
				driver.setFieldOpenChar(required(element, "fieldOpenChar"));
				driver.setFieldCloseChar(required(element, "fieldCloseChar"));
				driver.setDataOpenChar(required(element, "dataOpenChar"));
				driver.setDataCloseChar(required(element, "dataCloseChar"));
				read.add(driver);
			} catch (RuntimeException e) {
				// One bad entry leaves out that driver only, the others can still be used.
				log.warn("Driver '{}' in driver.xml skipped: {}", element.getChildText("driverName"), e.getMessage());
			}
		}

		drivers = read.toArray(new DatabaseDriver[0]);
		return drivers;
	}

	private static String required(Element element, String child) {
		String text = element.getChildText(child);
		if (text == null) {
			throw new IllegalArgumentException("<" + child + "> is missing");
		}
		return text;
	}

	/*
	 * @author: 		S.Oudmaijer.
	 * @description:	Saves the properties of one specific database driver.
	 */
	public void saveProperties(DatabaseDriver[] drivers, int id, DriverProperties properties) throws Exception {
		org.jdom.Element root = new org.jdom.Element("drivers");
		driverData.setRootElement(root);

		for (int i = 0; i < drivers.length; i++) {
			if (drivers[i].getId() == id) {
				drivers[i].setDriverURL(properties.url());
				drivers[i].setDriverClassName(properties.className());
				drivers[i].setFieldOpenChar(properties.identifierOpen());
				drivers[i].setFieldCloseChar(properties.identifierClose());
				drivers[i].setDataOpenChar(properties.stringOpen());
				drivers[i].setDataCloseChar(properties.stringClose());
			}

			root.addContent(new org.jdom.Element("driver")

				.addContent(new org.jdom.Element("id").setText(new String().valueOf(drivers[i].getId())))
				.addContent(new org.jdom.Element("driverName").setText(drivers[i].getDriverName()))
				.addContent(new org.jdom.Element("driverURL").setText(drivers[i].getDriverURL()))
				.addContent(new org.jdom.Element("driverClassName").setText(drivers[i].getDriverClassName()))
				//.addContent( new org.jdom.Element("driverFilePath").setText( drivers[i].getDriverFilePath() ))
				.addContent(new org.jdom.Element("fieldOpenChar").setText(drivers[i].getFieldOpenChar()))
				.addContent(new org.jdom.Element("fieldCloseChar").setText(drivers[i].getFieldCloseChar()))
				.addContent(new org.jdom.Element("dataOpenChar").setText(drivers[i].getDataOpenChar()))
				.addContent(new org.jdom.Element("dataCloseChar").setText(drivers[i].getDataCloseChar()))

			);
		}

		save(driverData);
	}

	/*
	 * @author: 		S.Oudmaijer.
	 * @description:	Saves the given JDOM document to the driver.xml file.
	 */
	public void save(Document document) throws Exception {
		XMLOutputter xmloutputter = new XMLOutputter();
		try (PrintWriter out = new PrintWriter(DataDirectory.file("conf/driver.xml"), java.nio.charset.StandardCharsets.UTF_8)) {
			xmloutputter.output(document, out);
		}
	}

	public void setDriverName(String driverName) {
		this.driverName = driverName;
	}

	public void setDriverURL(String driverURL) {
		this.driverURL = driverURL;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	//	public void setDriverFilePath(String driverFilePath) {
	//		this.driverFilePath = driverFilePath;
	//	}

	public void setFieldOpenChar(String fieldOpenChar) {
		this.fieldOpenChar = fieldOpenChar;
	}

	public void setFieldCloseChar(String fieldCloseChar) {
		this.fieldCloseChar = fieldCloseChar;
	}

	public void setDataOpenChar(String dataOpenChar) {
		this.dataOpenChar = dataOpenChar;
	}

	public void setDataCloseChar(String dataCloseChar) {
		this.dataCloseChar = dataCloseChar;
	}

	public String getDriverName() {
		return (this.driverName);
	}

	public String getDriverURL() {
		return (this.driverURL);
	}

	public String getDriverClassName() {
		return (this.driverClassName);
	}

	public String getDriverFilePath() {
		return "";
	}

	public String getFieldOpenChar() {
		return (this.fieldOpenChar);
	}

	public String getFieldCloseChar() {
		return (this.fieldCloseChar);
	}

	public String getDataOpenChar() {
		return (this.dataOpenChar);
	}

	public String getDataCloseChar() {
		return (this.dataCloseChar);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String toString() {
		return this.getDriverName();
	}
}
