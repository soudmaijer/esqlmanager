package nl.errorsoft.esql.domain.dialect;

import nl.errorsoft.esql.domain.ConnectionProfile;
import nl.errorsoft.esql.domain.ServerType;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.junit.jupiter.api.Assumptions;

class PostgresDialectTest extends DialectContractTest
{
	private static PostgreSQLContainer postgres;

	@BeforeAll
	static void startServer()
	{
		Assumptions.assumeTrue( DockerClientFactory.instance().isDockerAvailable(), "Docker is needed for this test" );
		postgres = new PostgreSQLContainer( "postgres:17" ).withDatabaseName( DATABASE );
		postgres.start();
	}

	protected ConnectionProfile profile()
	{
		ConnectionProfile cp = new ConnectionProfile();
		cp.setHost( postgres.getHost() );
		cp.setPort( String.valueOf( postgres.getMappedPort( 5432 ) ) );
		cp.setUsername( postgres.getUsername() );
		cp.setPassword( postgres.getPassword() );
		cp.setDatabases( DATABASE );
		cp.setServerType( new ServerType( ServerType.POSTGRES ) );
		return cp;
	}
}
