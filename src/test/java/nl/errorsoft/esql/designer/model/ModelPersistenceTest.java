package nl.errorsoft.esql.designer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import nl.errorsoft.esql.designer.ui.CommentObject;
import nl.errorsoft.esql.designer.ui.DatabaseObject;
import nl.errorsoft.esql.designer.ui.Field;
import nl.errorsoft.esql.designer.ui.TableObject;
import nl.errorsoft.esql.domain.DataType;
import nl.errorsoft.esql.domain.EsqlException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModelPersistenceTest {
	private static final String HOSTILE = "a < b & \"c\" 'd' > e";

	@Test
	void modelSurvivesARoundTripWithCharactersXmlCaresAbout(@TempDir Path dir) throws Exception {
		Model model = new Model(HOSTILE);
		model.setAuthor(HOSTILE);
		model.setComment(HOSTILE);
		DatabaseObject db = model.createDatabaseObject("shop");
		db.setDescription(HOSTILE);
		TableObject table = model.createTableObject("order<lines>");
		table.setComment(HOSTILE);
		table.setDescription(HOSTILE);
		Field field = new Field("id&key", new DataType("int", false, false, false, false, false, false, false, false), "11", HOSTILE, HOSTILE);
		field.primary = true;
		field.notnull = true;
		table.addField(field);
		CommentObject comment = model.createCommentObject(HOSTILE);
		table.addReference(db);
		comment.addReference(table);

		Model loaded = saveAndLoad(model, dir);

		assertEquals(HOSTILE, loaded.getName());
		assertEquals(HOSTILE, loaded.getAuthor());
		assertEquals(HOSTILE, loaded.getComment());
		assertEquals(model.getIdentifier(), loaded.getIdentifier());
		assertEquals(HOSTILE, ((DatabaseObject) loaded.getObjectByIdentifier(db.getIdentifier())).getDescription());

		TableObject loadedTable = (TableObject) loaded.getObjectByIdentifier(table.getIdentifier());
		assertEquals("order<lines>", loadedTable.getName());
		assertEquals(HOSTILE, loadedTable.getComment());
		assertEquals(HOSTILE, loadedTable.getDescription());
		Field loadedField = loadedTable.getFields()[0];
		assertEquals("id&key", loadedField.getName());
		assertEquals(HOSTILE, loadedField.getDefault());
		assertTrue(loadedField.primary && loadedField.notnull);
		assertTrue(loadedTable.getReferences().contains(loaded.getObjectByIdentifier(db.getIdentifier())));
		assertEquals(HOSTILE, ((CommentObject) loaded.getObjectByIdentifier(comment.getIdentifier())).getComment());
	}

	@Test
	void foreignKeysSurviveARoundTrip(@TempDir Path dir) throws Exception {
		Model model = new Model("fk");
		TableObject customer = table(model, "customer", "id", "region");
		TableObject order = table(model, "order", "id", "customer_id", "customer_region");
		model.addForeignKey(new ForeignKey(order, List.of("customer_id", "customer_region"), customer, List.of("id", "region"), "fk_<order>", "CASCADE", ""));

		Model loaded = saveAndLoad(model, dir);

		assertEquals(1, loaded.getForeignKeys().size());
		ForeignKey key = loaded.getForeignKeys().get(0);
		assertEquals("fk_<order>", key.name());
		assertEquals(order.getIdentifier(), key.from().getIdentifier());
		assertEquals(customer.getIdentifier(), key.to().getIdentifier());
		assertEquals(List.of("customer_id", "customer_region"), key.fromColumns());
		assertEquals(List.of("id", "region"), key.toColumns());
		assertEquals("CASCADE", key.onDelete());
		assertEquals("", key.onUpdate());
		assertEquals(1, loaded.foreignKeysOf((TableObject) loaded.getObjectByIdentifier(customer.getIdentifier())).size());
	}

	@Test
	void foreignKeysFollowTheirTablesAndFields() {
		Model model = new Model("fk");
		TableObject customer = table(model, "customer", "id", "name");
		TableObject order = table(model, "order", "id", "customer_id");
		TableObject line = table(model, "line", "id", "order_id");
		model.addForeignKey(new ForeignKey(order, List.of("customer_id"), customer, List.of("id"), "fk_customer", "", ""));
		model.addForeignKey(new ForeignKey(line, List.of("order_id"), order, List.of("id"), "fk_order", "", ""));

		Map<Field, String> before = names(customer);
		customer.getFields()[0].setName("customer_no");
		model.fieldsEdited(customer, before);
		assertEquals(List.of("customer_no"), model.foreignKeysOf(customer).get(0).toColumns());

		before = names(order);
		Field id = order.getFields()[0];
		order.removeAllFields();
		order.addField(id);
		model.fieldsEdited(order, before);
		assertEquals(List.of("fk_order"), model.getForeignKeys().stream().map(ForeignKey::name).toList());

		model.removeObject(line);
		assertTrue(model.getForeignKeys().isEmpty());
	}

	@Test
	void newFilesAreVersionTwo() {
		assertTrue(new Model("m").getModelXML().contains("<version>0.2</version>"));
	}

	@Test
	void versionOneFileStillLoads() throws Exception {
		Model model = new Model("x").loadModel(new File(getClass().getResource("/designer/shop-0.1.edm").toURI()));

		assertEquals("Shop", model.getName());
		assertEquals(5, model.getIdentifier());
		TableObject customer = (TableObject) model.getObjectByIdentifier(2);
		assertEquals("customer", customer.getName());
		assertEquals(2, customer.getFields().length);
		assertTrue(customer.getFields()[0].primary);
		assertTrue(customer.getReferences().contains(model.getObjectByIdentifier(1)));
		assertEquals("Remember the orders", ((CommentObject) model.getObjectByIdentifier(3)).getComment());
		assertEquals(30, customer.getY());
	}

	@Test
	void unknownVersionIsRefused(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("future.edm");
		Files.writeString(file, "<model><version>9.0</version></model>", StandardCharsets.UTF_8);

		assertThrows(EsqlException.class, () -> new Model("x").loadModel(file.toFile()));
	}

	@Test
	void missingSectionsMeanEmpty(@TempDir Path dir) throws Exception {
		Path file = dir.resolve("bare.edm");
		Files.writeString(file, "<model><version>0.2</version><name>bare</name></model>", StandardCharsets.UTF_8);

		Model model = new Model("x").loadModel(file.toFile());
		assertEquals("bare", model.getName());
		assertTrue(model.getObjects().isEmpty());
	}

	private static TableObject table(Model model, String name, String... fields) {
		TableObject table = model.createTableObject(name);
		for (String field : fields) {
			table.addField(new Field(field, new DataType("int", false, false, false, false, false, false, false, false), "", "", ""));
		}
		return table;
	}

	private static Map<Field, String> names(TableObject table) {
		Map<Field, String> names = new IdentityHashMap<>();
		for (Field field : table.getFields()) {
			names.put(field, field.getName());
		}
		return names;
	}

	static Model saveAndLoad(Model model, Path dir) throws Exception {
		Path file = dir.resolve("model.edm");
		Files.writeString(file, model.getModelXML(), StandardCharsets.UTF_8);
		return model.loadModel(file.toFile());
	}
}
