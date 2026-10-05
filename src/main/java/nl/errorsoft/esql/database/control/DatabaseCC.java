//Source file: d:\\roseoutput\\esql\\esql\\database\\DatabaseCC.java

package nl.errorsoft.esql.database.control;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.database.DatabaseService;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.ui.TableListView;

import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.ui.DatabaseTreeView;
import nl.errorsoft.esql.ui.icon.ImageLoader;

public class DatabaseCC {
	private ConnectionWindowCC cwcc;

	/**
	* @roseuid 3E05A70C031D
	*/
	public DatabaseCC(ConnectionWindowCC cwcc) {
		this.cwcc = cwcc;
	}

	public java.util.List<Database> getDatabases() throws Exception {
		return service().getDatabases();
	}

	public Database createDatabase(String name) throws Exception {
		return service().createDatabase(name);
	}

	public void dropDatabase(Database data) throws Exception {
		service().dropDatabase(data);
	}

	public java.util.List<Table> getTables(Database database) throws Exception {
		return service().getTables(database);
	}

	public java.util.List<Schema> getSchemas(Database database) throws Exception {
		return service().getSchemas(database);
	}

	public java.util.List<Table> getTables(Schema schema) throws Exception {
		return service().getTables(schema);
	}

	public Schema createSchema(Database database, String name) throws Exception {
		return service().createSchema(database, name);
	}

	public void dropSchema(Schema schema) throws Exception {
		service().dropSchema(schema);
	}

	private DatabaseService service() throws Exception {
		return cwcc.getContext().databases();
	}

	public DatabaseTreeView getDatabaseTreeView() throws Exception {
		DatabaseTreeView dbtv = new DatabaseTreeView(cwcc.getTitle(), cwcc.getConnectionProfile().getServerType().iconName());
		dbtv.loadDatabases(getDatabases());
		return dbtv;
	}

	public TableListView getTableListView(java.util.List<Table> tables) throws Exception {
		TableListView tlv = new TableListView(this);
		tlv.loadDatabases(tables);
		return tlv;
	}

	public void tableSelected(Table table) {
		cwcc.selectTableInTree(table);
	}

}
