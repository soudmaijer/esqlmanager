package nl.errorsoft.esql.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PrivilegeGroupTest {
	@Test
	void groupsByName() {
		assertEquals(PrivilegeGroup.DATA, PrivilegeGroup.of("SELECT"));
		assertEquals(PrivilegeGroup.DATA, PrivilegeGroup.of("truncate"));
		assertEquals(PrivilegeGroup.STRUCTURE, PrivilegeGroup.of("ALTER"));
		assertEquals(PrivilegeGroup.ADMINISTRATION, PrivilegeGroup.of("SUPERUSER"));
		assertEquals(PrivilegeGroup.ADMINISTRATION, PrivilegeGroup.of("RELOAD"));
	}
}
