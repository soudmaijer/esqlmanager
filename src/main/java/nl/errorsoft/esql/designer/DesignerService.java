package nl.errorsoft.esql.designer;

import java.util.List;

import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.table.TableService;

/** Creates the databases and tables of a designed model on the server. */
public class DesignerService
{
	private final DatabaseService databases;
	private final TableService tables;

	public DesignerService( DatabaseConnection dbc )
	{
		this.databases = new DatabaseService( dbc );
		this.tables = new TableService( dbc );
	}

	/**
	 * Creates what is missing, generating a model again leaves the databases and tables that are already there alone.
	 * The step callback is called for every database, table and column handled.
	 */
	public void generate( List<DesignedDatabase> model, Runnable step ) throws Exception
	{
		for ( DesignedDatabase designed : model )
		{
			Database database = new Database( designed.name() );

			if ( !databases.exists( database ) )
				databases.createDatabase( database.getName() );

			databases.use( database );
			step.run();

			for ( DesignedTable table : designed.tables() )
			{
				step.run();

				if ( !tables.exists( database, table.name() ) )
					tables.createTable( database, table.name(), table.columns(), table.type(), table.comment() );

				for ( int i = 0; i < table.columns().size(); i++ )
					step.run();
			}
		}
	}
}
