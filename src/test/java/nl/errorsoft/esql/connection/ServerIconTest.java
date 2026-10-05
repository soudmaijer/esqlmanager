package nl.errorsoft.esql.connection;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import nl.errorsoft.esql.app.ApplicationContext;

class ServerIconTest {

	@Test
	void everyServerTypeHasItsOwnBrandIcon() {
		Set<String> names = new HashSet<>();

		for (ServerType type : ServerType.getServerTypes()) {
			String name = type.iconName();
			assertNotEquals("pc", name, type.getDescription());
			assertTrue(names.add(name), "icon used twice: " + name);

			FlatSVGIcon icon = assertInstanceOf(FlatSVGIcon.class, ApplicationContext.get().imageLoader().getIcon(name), name);
			assertTrue(icon.hasFound(), "svg of " + name + " not found");
			assertTrue(icon.getIconWidth() == 18 && icon.getIconHeight() == 18, name);
		}
	}
}
