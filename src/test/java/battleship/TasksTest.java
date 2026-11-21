package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Scanner;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TasksTest
 *
 * @author Your Name
 * Date: 07/01/2026
 * Time: 00:53
 */
class TasksTest {

	@BeforeEach
	void setUp() {
	}

	@AfterEach
	void tearDown() {
	}

	@Test
	@DisplayName("Le poisicao numerica")
	void readPositionNumeric() {
		Position position = Tasks.readPosition(new Scanner("3 7"));
		assertAll(
				() -> assertEquals(3, position.getRow()),
				() -> assertEquals(7, position.getColumn())
		);
	}

	@Test
	@DisplayName("Le um navio conhecido")
	void readShip() {
		Ship ship = Tasks.readShip(new Scanner("barca 2 4 s"));
		assertAll(
				() -> assertNotNull(ship),
				() -> assertEquals("Barca", ship.getCategory()),
				() -> assertEquals(Compass.SOUTH, ship.getBearing())
		);
	}

	@Test
	@DisplayName("Le uma posicao classica compacta")
	void readPosition() {
		Position position = Tasks.readPosition(new Scanner("3 7"));
		assertAll(
				() -> assertEquals(3, position.getRow()),
				() -> assertEquals(7, position.getColumn())
		);
	}

	@Test
	@DisplayName("Le posicao classica compacta")
	void readClassicPositionCompact() {
		IPosition position = Tasks.readClassicPosition(new Scanner("A3"));
		assertEquals("A3", position.toString());
	}

	@Test
	@DisplayName("Le posicao classica com linha e coluna separadas")
	void readClassicPositionSeparated() {
		IPosition position = Tasks.readClassicPosition(new Scanner("J 10"));
		assertEquals("J10", position.toString());
	}

	@Test
	@DisplayName("Rejeita posicao classica vazia")
	void rejectsEmptyClassicPosition() {
		assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("")));
	}

	@Test
	@DisplayName("Rejeita formato classico invalido")
	void rejectsInvalidClassicPosition() {
		assertThrows(IllegalArgumentException.class,
				() -> Tasks.readClassicPosition(new Scanner("123")));
	}

	@Test
	@DisplayName("Apresenta a ajuda do menu")
	void menuHelpIsPrinted() {
		PrintStream original = System.out;
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
			System.setOut(capture);
			Tasks.menuHelp();
		} finally {
			System.setOut(original);
		}

		String text = output.toString(StandardCharsets.UTF_8);
		assertAll(
				() -> assertTrue(text.contains("AJUDA DO MENU")),
				() -> assertTrue(text.contains("gerafrota")),
				() -> assertTrue(text.contains("desisto"))
		);
	}
}