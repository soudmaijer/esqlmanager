package nl.errorsoft.esql.dialect;

import nl.errorsoft.esql.connection.ConnectionProfile;
import nl.errorsoft.esql.connection.ServerType;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.junit.jupiter.api.Assumptions;

class PostgresDialectTest extends DialectContractTest {
	private static PostgreSQLContainer postgres;

	@BeforeAll
	static void startServer() {
		Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is needed for this test");
		postgres = new PostgreSQLContainer("postgres:17").withDatabaseName(DATABASE);
		postgres.start();
	}

	protected ConnectionProfile profile() {
		ConnectionProfile profile = new ConnectionProfile();
		profile.setHost(postgres.getHost());
		profile.setPort(String.valueOf(postgres.getMappedPort(5432)));
		profile.setUsername(postgres.getUsername());
		profile.setPassword(postgres.getPassword());
		profile.setDatabases(DATABASE);
		profile.setServerType(new ServerType(ServerType.POSTGRES));
		return profile;
	}

	@org.junit.jupiter.api.Test
	void writesTheExplainStatements() {
		Dialect dialect = profile().getServerType().getDialect();
		org.junit.jupiter.api.Assertions.assertTrue(dialect.supports(Dialect.Feature.EXPLAIN));
		org.junit.jupiter.api.Assertions.assertEquals("EXPLAIN (FORMAT JSON) SELECT 1", dialect.explainSql(" SELECT 1 ", false));
		org.junit.jupiter.api.Assertions.assertEquals("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) SELECT 1", dialect.explainSql("SELECT 1", true));
	}
}
