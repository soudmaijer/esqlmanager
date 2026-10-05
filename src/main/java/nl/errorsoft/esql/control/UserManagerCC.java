package nl.errorsoft.esql.control;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.domain.Database;
import nl.errorsoft.esql.domain.DatabaseUser;
import nl.errorsoft.esql.domain.GrantTarget;
import nl.errorsoft.esql.domain.Table;
import nl.errorsoft.esql.domain.dialect.Dialect;
import nl.errorsoft.esql.domain.dialect.UserAdmin;
import nl.errorsoft.esql.gui.ESQLManagerUI;
import nl.errorsoft.esql.gui.UserManagerUI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UserManagerCC
{
	private static final Logger log = LogManager.getLogger( UserManagerCC.class );

	private ConnectionWindowCC cwcc;

	public UserManagerCC( ConnectionWindowCC cwcc )
	{
		this.cwcc = cwcc;
	}

	public void startUI( ESQLManagerUI emui )
	{
		try
		{
			if( !getDialect().supports( Dialect.Feature.USER_MANAGER ) )
			{	cwcc.getUI().showErrorMessage( "The user manager is not available for this database" );
				return;
			}

			emui.updateStatus( "Starting usermanager...", true );
			UserManagerUI ui = new UserManagerUI( emui, this );
			emui.updateStatus( "Ready...", false );
			ui.setVisible( true );
		}
		catch( Exception e )
		{
			log.error( e.getMessage(), e );
			cwcc.getUI().showErrorMessage( "Can't start the user manager: " + e.getMessage() );
		}
	}

	public boolean usesHost()
	{
		return getUserAdmin().usesHost();
	}

	public List<String> getPrivileges( GrantTarget.Scope scope )
	{
		return getUserAdmin().getPrivileges( scope );
	}

	public List<DatabaseUser> listUsers() throws Exception
	{
		return getUserAdmin().listUsers( connection() );
	}

	public void createUser( DatabaseUser user, String password ) throws Exception
	{
		getUserAdmin().createUser( connection(), user, password );
	}

	public void changePassword( DatabaseUser user, String password ) throws Exception
	{
		getUserAdmin().changePassword( connection(), user, password );
	}

	public void dropUser( DatabaseUser user ) throws Exception
	{
		getUserAdmin().dropUser( connection(), user );
	}

	public Set<String> getGrants( DatabaseUser user, GrantTarget target ) throws Exception
	{
		return getUserAdmin().getGrants( connection(), user, target );
	}

	public void setGrants( DatabaseUser user, GrantTarget target, Set<String> privileges ) throws Exception
	{
		getUserAdmin().setGrants( connection(), user, target, privileges );
	}

	public List<String> getDatabaseNames() throws Exception
	{
		List<String> names = new ArrayList<String>();

		for( Object database : new DatabaseCC( cwcc ).getDatabases() )
			names.add( database.toString() );

		return names;
	}

	public List<String> getTableNames( String databaseName ) throws Exception
	{
		DatabaseCC databaseCC = new DatabaseCC( cwcc );
		List<String> names = new ArrayList<String>();

		for( Object database : databaseCC.getDatabases() )
		{
			if( database.toString().equals( databaseName ) )
			{
				for( Object table : databaseCC.getTables( (Database)database ) )
					names.add( ( (Table)table ).getName() );
			}
		}
		return names;
	}

	private DatabaseConnection connection() throws Exception
	{
		return cwcc.getDatabaseConnection();
	}

	private Dialect getDialect()
	{
		return cwcc.getConnectionProfile().getServerType().getDialect();
	}

	private UserAdmin getUserAdmin()
	{
		return getDialect().getUserAdmin();
	}
}
