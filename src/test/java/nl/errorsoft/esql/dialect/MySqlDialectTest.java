package nl.errorsoft.esql.dialect;

import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerType;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.mysql.MySQLContainer;

class MySqlDialectTest extends DialectContractTest {
	private static MySQLContainer mysql;

	@BeforeAll
	static void startServer() {
		Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is needed for this test");
		mysql = new MySQLContainer("mysql:8").withDatabaseName(DATABASE).withUsername("root").withPassword("test");
		mysql.start();
	}

	protected ConnectionProfile profile() {
		ConnectionProfile cp = new ConnectionProfile();
		cp.setHost(mysql.getHost());
		cp.setPort(String.valueOf(mysql.getMappedPort(3306)));
		cp.setUsername("root");
		cp.setPassword("test");
		cp.setDatabases(DATABASE);
		cp.setServerType(new ServerType(ServerType.MY_SQL));
		return cp;
	}
}
