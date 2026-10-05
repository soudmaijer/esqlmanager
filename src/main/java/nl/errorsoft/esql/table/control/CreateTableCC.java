package nl.errorsoft.esql.table.control;

import nl.errorsoft.esql.table.CreateColumn;
import nl.errorsoft.esql.table.DataType;
import nl.errorsoft.esql.table.Table;
import nl.errorsoft.esql.table.ui.CreateTable;

import nl.errorsoft.esql.app.ApplicationContext;

import nl.errorsoft.esql.connection.control.ConnectionWindowCC;
import nl.errorsoft.esql.database.control.DatabaseCC;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.errorsoft.esql.dialect.Dialect;
import nl.errorsoft.esql.table.TableService;
import nl.errorsoft.esql.database.Database;
import java.util.Vector;

public class CreateTableCC {
	private ConnectionWindowCC cwcc;
	private static final Logger log = LogManager.getLogger(CreateTableCC.class);

	public CreateTableCC(ConnectionWindowCC cwcc) {
		this.cwcc = cwcc;
	}

	/*
	 	List all databases, so user can choose database to create table on
	*/
	public Vector getDatabases() {
		try {
			DatabaseCC dbc = new DatabaseCC(cwcc);
			return dbc.getDatabases();
		} catch (Exception e) {
			ApplicationContext.get().errors().report("Load databases", e);
			return new Vector();
		}
	}

	/*
	 	List all tabletypes, empty when the server has no such choice
	*/
	public String[] getTableTypes() {
		return cwcc.getConnectionProfile().getServerType().getDialect().getTableTypes();
	}

	/*
	 	List all datatypes
	*/
	public DataType[] getDatatypes() {
		return cwcc.getConnectionProfile().getServerType().getDataTypes();
	}

	/*
		Save and close, moet in de domein class!!!!!!!!!
	*/
	public void createTable(String name, String database, String comment, String type, CreateTable ct, Vector columns) {
		if (name.trim().length() == 0) {
			ct.showErrorMessage("Tablename missing. You must enter a tablename in order to create a table.");
			return;
		}
		if (columns.size() == 0) {
			ct.showErrorMessage("You didn't add any columns to the table. Please add some fields to the table prior to generating it.");
			return;
		}
		try {
			java.util.List<CreateColumn> list = new java.util.ArrayList<>();
			for (int i = 0; i < columns.size(); i++) {
				list.add((CreateColumn) columns.get(i));
			}
			cwcc.getContext().tables().createTable(new Database(database), name, list, type, comment);
			ct.dispose();
			cwcc.reloadSelectedDatabase();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(ct, "Create table", e);
		}
	}

	public void modifyTable(CreateTable ct, Table t, String tableName, String tableType, String tableComment) {
		try {
			cwcc.getContext().tables().modifyTable(t, tableName, tableType, tableComment);
			ct.dispose();
			//cwcc.reloadSelectedDatabase();
		} catch (Exception e) {
			ApplicationContext.get().errors().report(ct, "Modify table", e);
		}
	}

	/*
		Close
	*/
	public void closeDialog(CreateTable ct) {
		ct.dispose();
	}
}
