package nl.errorsoft.esql.app;

public class ESQLManager {
	private final int LE = 0;
	private final int PRO = 1;
	private final int majorVersion = 1;
	private String appName = "eSQLManager";
	private final String appVersion = "1.0";
	private final int appBuild = 64;
	private final int appType = PRO;

	public ESQLManager() {
		if (appType == PRO) {
			appName = appName + " Pro";
		} else {
			appName = appName + " LE";
		}
	}

	public int getMajorVersion() {
		return this.majorVersion;
	}

	public String getAppName() {
		return appName;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public int getAppBuild() {
		return appBuild;
	}

	public boolean isPro() {
		if (appType == PRO) {
			return true;
		}
		return false;
	}
}
