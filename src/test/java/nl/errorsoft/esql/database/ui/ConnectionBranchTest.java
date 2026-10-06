package nl.errorsoft.esql.database.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.junit.jupiter.api.Test;

import nl.errorsoft.esql.database.Database;
import nl.errorsoft.esql.database.Schema;
import nl.errorsoft.esql.table.Table;

/** Two connections in one tree, each changing only its own branch. */
class ConnectionBranchTest {
	private final DefaultMutableTreeNode root = new DefaultMutableTreeNode("connections");
	private final DefaultMutableTreeNode postgres = new DefaultMutableTreeNode("postgres@localhost");
	private final DefaultMutableTreeNode mysql = new DefaultMutableTreeNode("root@localhost");
	private final JTree tree = new JTree(new DefaultTreeModel(root));
	private final ConnectionBranch postgresBranch = new ConnectionBranch(tree, postgres);
	private final ConnectionBranch mysqlBranch = new ConnectionBranch(tree, mysql);

	ConnectionBranchTest() {
		root.add(postgres);
		root.add(mysql);
		((DefaultTreeModel) tree.getModel()).reload();
	}

	@Test
	void databasesWithTheSameNameStayInTheirOwnBranch() {
		Database pgShop = new Database("shop");
		Database myShop = new Database("shop");
		postgresBranch.loadDatabases(List.of(pgShop, new Database("postgres")));
		mysqlBranch.loadDatabases(List.of(myShop));

		Schema sales = new Schema(pgShop, "sales");
		postgresBranch.loadSchemas(pgShop, List.of(sales));
		postgresBranch.loadTables(sales, List.of(table(sales, "orders")));
		mysqlBranch.loadTables(myShop, List.of(table(myShop, "customers")));

		assertEquals(List.of("shop", "postgres"), postgresBranch.databaseNames());
		assertEquals(List.of("shop"), mysqlBranch.databaseNames());
		assertEquals("orders", leaf(postgres.getChildAt(0).getChildAt(0).getChildAt(0)));
		assertEquals("customers", leaf(mysql.getChildAt(0).getChildAt(0)));
	}

	@Test
	void aLoadInOneConnectionKeepsTheSelectionInTheOther() {
		Database pgShop = new Database("shop");
		Database myShop = new Database("shop");
		postgresBranch.loadDatabases(List.of(pgShop));
		mysqlBranch.loadDatabases(List.of(myShop));
		postgresBranch.selectDatabase(pgShop);

		mysqlBranch.loadTables(myShop, List.of(table(myShop, "customers")));

		assertSame(postgres.getChildAt(0), tree.getSelectionPath().getLastPathComponent());
	}

	@Test
	void reloadingTheSelectedTableKeepsItSelected() {
		Database shop = new Database("shop");
		Table orders = table(shop, "orders");
		mysqlBranch.loadDatabases(List.of(shop));
		mysqlBranch.loadTables(shop, List.of(orders));
		mysqlBranch.selectTable(orders);

		mysqlBranch.loadTables(shop, List.of(orders, table(shop, "customers")));

		assertSame(mysql.getChildAt(0), tree.getSelectionPath().getLastPathComponent());
	}

	@Test
	void deletingADatabaseLeavesTheOtherConnectionAlone() {
		Database pgShop = new Database("shop");
		postgresBranch.loadDatabases(List.of(pgShop));
		mysqlBranch.loadDatabases(List.of(new Database("shop")));

		postgresBranch.deleteDatabase(pgShop);

		assertEquals(List.of(), postgresBranch.databaseNames());
		assertEquals(List.of("shop"), mysqlBranch.databaseNames());
		assertEquals(2, root.getChildCount());
	}

	@Test
	void selectingATableSelectsItsNodeOnTheFullPath() {
		Database shop = new Database("shop");
		Table orders = table(shop, "orders");
		mysqlBranch.loadDatabases(List.of(shop));
		mysqlBranch.loadTables(shop, List.of(orders));

		mysqlBranch.selectTable(orders);

		assertEquals(4, tree.getSelectionPath().getPathCount());
		assertSame(root, tree.getSelectionPath().getPathComponent(0));
	}

	@Test
	void aLazyBranchLoadsWhatIsBelowANodeOnlyWhenAskedTo() {
		DefaultMutableTreeNode server = new DefaultMutableTreeNode("lazy");
		root.add(server);
		ConnectionBranch lazy = new ConnectionBranch(tree, server, true);
		Database shop = new Database("shop");
		Table orders = table(shop, "orders");

		lazy.loadDatabases(List.of(shop));
		DefaultMutableTreeNode shopNode = (DefaultMutableTreeNode) server.getChildAt(0);
		assertTrue(ConnectionBranch.needsLoading(shopNode));

		lazy.loadTables(shop, List.of(orders));
		assertFalse(ConnectionBranch.needsLoading(shopNode));
		assertTrue(ConnectionBranch.needsLoading((DefaultMutableTreeNode) shopNode.getChildAt(0)));
	}

	@Test
	void aBranchOfADialogHasNoPlaceholders() {
		Database shop = new Database("shop");
		mysqlBranch.loadDatabases(List.of(shop));
		assertEquals(0, mysql.getChildAt(0).getChildCount());
	}

	private static Table table(Database database, String name) {
		Table table = new Table(database);
		table.setName(name);
		return table;
	}

	private static Table table(Schema schema, String name) {
		Table table = new Table(schema);
		table.setName(name);
		return table;
	}

	private static String leaf(Object node) {
		return ((DefaultMutableTreeNode) node).getUserObject().toString();
	}
}
