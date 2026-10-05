package nl.errorsoft.esql.designer.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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

	static Model saveAndLoad(Model model, Path dir) throws Exception {
		Path file = dir.resolve("model.edm");
		Files.writeString(file, model.getModelXML(), StandardCharsets.UTF_8);
		return model.loadModel(file.toFile());
	}
}
