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
		ConnectionProfile profile = new ConnectionProfile();
		profile.setHost(mysql.getHost());
		profile.setPort(String.valueOf(mysql.getMappedPort(3306)));
		profile.setUsername("root");
		profile.setPassword("test");
		profile.setDatabases(DATABASE);
		profile.setServerType(new ServerType(ServerType.MY_SQL));
		return profile;
	}

	@org.junit.jupiter.api.Test
	void writesTheExplainStatements() {
		Dialect dialect = profile().getServerType().getDialect();
		org.junit.jupiter.api.Assertions.assertTrue(dialect.supports(Dialect.Feature.EXPLAIN));
		org.junit.jupiter.api.Assertions.assertEquals("EXPLAIN FORMAT=JSON SELECT 1", dialect.explainSql(" SELECT 1 ", false));
		org.junit.jupiter.api.Assertions.assertEquals("EXPLAIN ANALYZE SELECT 1", dialect.explainSql("SELECT 1", true));
	}
}
