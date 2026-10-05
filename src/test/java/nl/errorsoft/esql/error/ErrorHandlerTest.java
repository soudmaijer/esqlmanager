package nl.errorsoft.esql.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class ErrorHandlerTest {
	@Test
	void namesTheActionThatFailedAndTheMostSpecificCause() {
		Exception error = new RuntimeException("wrapper", new SQLException("table \"x\" does not exist"));
		assertEquals("Drop table failed: table \"x\" does not exist", ErrorHandler.message("Drop table", error));
	}

	@Test
	void doesNotRepeatFailedAfterAnError() {
		assertEquals("Unexpected error: boom", ErrorHandler.message("Unexpected error", new IllegalStateException("boom")));
	}
}
