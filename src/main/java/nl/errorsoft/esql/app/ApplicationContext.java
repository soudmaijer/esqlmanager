package nl.errorsoft.esql.app;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nl.errorsoft.esql.connection.ConnectionContext;
import nl.errorsoft.esql.data.DatabaseConnection;
import nl.errorsoft.esql.ui.ImageLoader;

/**
 * The one place components are resolved from. It holds what exists once per application (images, settings) and the
 * {@link ConnectionContext} of every open connection, which holds the services that work on that connection.
 */
public final class ApplicationContext {
	private static final ApplicationContext INSTANCE = new ApplicationContext();

	private final Map<DatabaseConnection, ConnectionContext> connections = new ConcurrentHashMap<>();
	private final ErrorHandler errors = new ErrorHandler();
	private ImageLoader images;
	private Settings settings;

	private ApplicationContext() {
	}

	public static ApplicationContext get() {
		return INSTANCE;
	}

	public synchronized ImageLoader imageLoader() {
		if (images == null) {
			images = new ImageLoader("icons/");
			images.addImage("redLight.gif", "redLight");
			images.addImage("greenLight.gif", "greenLight");
			images.addImage("advance.gif", "advance");
			images.addImage("add_field.gif", "imgAddField");
			images.addImage("cascade.gif", "imgCascade");
			images.addImage("connect.gif", "imgConnect");
			images.addImage("computer.gif", "pc");
			images.addImage("create_table.gif", "imgCreateTable");
			images.addImage("create_db.gif", "imgCreateDb");
			images.addImage("db.gif", "dbimg");
			images.addImage("db_select_20x20.gif", "db_select_20x20");
			images.addImage("dbselect.gif", "dbimgsel");
			images.addImage("decrease.gif", "decrease");
			images.addImage("delete_field.gif", "imgDeleteField");
			images.addImage("delete_row.gif", "imgDeleteRow");
			images.addImage("disconnect.gif", "imgDisconnect");
			images.addImage("do_run_query.gif", "imgDoRunQuery");
			images.addImage("drop_db.gif", "imgDropDb");
			images.addImage("drop_table.gif", "imgDropTable");
			images.addImage("fd_select_20x20.gif", "fd_select_20x20");
			images.addImage("field.gif", "fldimg");
			images.addImage("fieldsel.gif", "fldimgsel");
			images.addImage("ico16x16.gif", "windowIcon");
			images.addImage("key.gif", "keyimg");
			images.addImage("keysel.gif", "keyimgsel");
			images.addImage("leaf.gif", "tbimg");
			images.addImage("leafselect.gif", "tbimgsel");
			images.addImage("new_row.gif", "imgNewRow");
			images.addImage("rt_select_20x20.gif", "rt_select_20x20");
			images.addImage("run_query.gif", "imgRunQuery");
			images.addImage("save.gif", "imgSave");
			images.addImage("sortdown.gif", "sortdown");
			images.addImage("sortup.gif", "sortup");
			images.addImage("tb_select_20x20.gif", "tb_select_20x20");
			images.addImage("tile_horizontal.gif", "imgTileHorizontal");
			images.addImage("tile_vertical.gif", "imgTileVertical");
			images.addImage("update.gif", "imgUpdateRow");
			images.addImage("users.gif", "imgUserManager");
			images.addImage("last.gif", "imgLast");
			images.addImage("first.gif", "imgFirst");
			images.addImage("hasindex.gif", "imgHasIndex");
			images.addImage("hasindexsel.gif", "imgHasIndexSel");
			images.addImage("arrow.gif", "arrow");
			images.addImage("leaf_new.gif", "tbnew");
			images.addImage("leaf_rem.gif", "tbrem");
			images.addImage("leaf_edit.gif", "tbedit");
			images.addImage("des_add_db.gif", "add_database");
			images.addImage("des_add_tb.gif", "add_table");
			images.addImage("des_add_cm.gif", "add_comment");
			images.addImage("des_prop_obj.gif", "des_properties");
			images.addImage("des_open.gif", "des_open");
			images.addImage("des_new.gif", "des_new");
			images.addImage("des_save.gif", "des_save");
			images.addImage("des_check.gif", "des_check");
			images.addImage("des_check1.gif", "check_off");
			images.addImage("des_check2.gif", "check_good");
			images.addImage("des_check3.gif", "check_error");
			images.addImage("../images/splash.gif", "esql");
		}
		return images;
	}

	public ErrorHandler errors() {
		return errors;
	}

	public synchronized Settings settings() {
		if (settings == null) {
			settings = new Settings();
		}
		return settings;
	}

	/** The services for a connection, created the first time the connection is asked for. */
	public ConnectionContext connection(DatabaseConnection connection) {
		return connections.computeIfAbsent(connection, ConnectionContext::new);
	}

	/** Forgets the services of a connection that has been closed. */
	public void release(DatabaseConnection connection) {
		if (connection != null) {
			connections.remove(connection);
		}
	}
}
