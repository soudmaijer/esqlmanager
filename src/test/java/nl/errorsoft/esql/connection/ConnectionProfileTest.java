package nl.errorsoft.esql.connection;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ConnectionProfileTest {

	private static ConnectionProfile profile() {
		ConnectionProfile profile = ConnectionProfile.plain();
		profile.setName("Local");
		profile.setHost("localhost");
		profile.setPort("5432");
		profile.setUsername("postgres");
		profile.setPassword("secret");
		profile.setServerType(new ServerType(ServerType.POSTGRES));
		profile.setSelection(DatabaseSelection.parse("shop"));
		return profile;
	}

	@Test
	void identicalSettingsAreTheSame() {
		assertTrue(profile().sameSettings(profile()));
	}

	@Test
	void aCopyKeepsTheSettingsButNotTheName() {
		ConnectionProfile original = profile();

		assertFalse(original.sameSettings(original.copyAs("Local copy")));
	}

	@Test
	void everyChangedSettingIsADifference() {
		ConnectionProfile changedName = profile();
		changedName.setName("Other");
		ConnectionProfile changedPort = profile();
		changedPort.setPort("5433");
		ConnectionProfile changedType = profile();
		changedType.setServerType(new ServerType(ServerType.MY_SQL));
		ConnectionProfile changedSelection = profile();
		changedSelection.setSelection(DatabaseSelection.parse("shop,crm"));
		ConnectionProfile changedAutoConnect = profile();
		changedAutoConnect.setAutoConnect(true);
		ConnectionProfile changedSavePassword = profile();
		changedSavePassword.setSavePassword(false);

		for (ConnectionProfile changed : new ConnectionProfile[]{changedName, changedPort, changedType, changedSelection, changedAutoConnect,
			changedSavePassword}) {
			assertFalse(profile().sameSettings(changed));
		}
	}

	@Test
	void theLastUsedFlagIsNotASetting() {
		ConnectionProfile lastUsed = profile();
		lastUsed.setLastUsed(true);

		assertTrue(profile().sameSettings(lastUsed));
	}
}
