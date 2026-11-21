package battleship;

import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the Frigate class.
 * Author: ${user.name}
 * Date: ${current_date}
 * Time: ${current_time}
 * Cyclomatic Complexity for each method:
 * - Constructor: 5
 * - getSize: 1
 * - stillFloating: 2
 * - getPositions: 2
 * - getTopMostPos: 2
 * - getBottomMostPos: 2
 * - getLeftMostPos: 2
 * - getRightMostPos: 2
 */
@DisplayName("Testes da classe Frigate")
public class FrigateTest {

	private Frigate frigate;

	@BeforeEach
	void setUp() {
		frigate = new Frigate(Compass.NORTH, new Position(5, 5));
	}

	@AfterEach
	void tearDown() {
		frigate = null;
	}

	/**
	 * Test for the constructor with NORTH bearing.
	 * Cyclomatic Complexity: 5
	 */
	@Test
	@DisplayName("Constrói uma fragata orientada a Norte")
	void testConstructorNorth() {
		assertNotNull(frigate, "Error: Frigate instance should not be null.");
		assertEquals("Fragata", frigate.getCategory(), "Error: Frigate category should be 'Fragata'.");
		assertEquals(Compass.NORTH, frigate.getBearing(), "Error: Frigate bearing is incorrect.");
		assertEquals(4, frigate.getSize(), "Error: Frigate size should be 4.");

		List<IPosition> positions = frigate.getPositions();
		assertEquals(4, positions.size(), "Error: Frigate should have exactly 4 positions.");
		assertEquals(new Position(5, 5), positions.get(0), "Error: First position is incorrect for NORTH.");
		assertEquals(new Position(6, 5), positions.get(1), "Error: Second position is incorrect for NORTH.");
		assertEquals(new Position(7, 5), positions.get(2), "Error: Third position is incorrect for NORTH.");
		assertEquals(new Position(8, 5), positions.get(3), "Error: Fourth position is incorrect for NORTH.");
	}

	/**
	 * Test for the constructor with EAST bearing.
	 */
	@Test
	@DisplayName("Constrói uma fragata orientada a Este")
	void testConstructorEast() {
		frigate = new Frigate(Compass.EAST, new Position(5, 5));
		List<IPosition> positions = frigate.getPositions();

		assertNotNull(frigate, "Error: Frigate instance should not be null.");
		assertEquals(4, positions.size(), "Error: Frigate should have exactly 4 positions.");
		assertEquals(new Position(5, 5), positions.get(0), "Error: First position is incorrect for EAST.");
		assertEquals(new Position(5, 6), positions.get(1), "Error: Second position is incorrect for EAST.");
		assertEquals(new Position(5, 7), positions.get(2), "Error: Third position is incorrect for EAST.");
		assertEquals(new Position(5, 8), positions.get(3), "Error: Fourth position is incorrect for EAST.");
	}

	/**
	 * Test for the constructor with WEST bearing.
	 */
	@Test
	@DisplayName("Constrói uma fragata orientada a Oeste")
	void testConstructorWest() {
		frigate = new Frigate(Compass.WEST, new Position(5, 5));
		List<IPosition> positions = frigate.getPositions();

		assertNotNull(frigate, "Error: Frigate instance should not be null.");
		assertEquals(4, positions.size(), "Error: Frigate should have exactly 4 positions.");
		assertEquals(new Position(5, 5), positions.get(0), "Error: First position is incorrect for WEST.");
		assertEquals(new Position(5, 6), positions.get(1), "Error: Second position is incorrect for WEST.");
		assertEquals(new Position(5, 7), positions.get(2), "Error: Third position is incorrect for WEST.");
		assertEquals(new Position(5, 8), positions.get(3), "Error: Fourth position is incorrect for WEST.");
	}

	/**
	 * Test for the getSize method.
	 * Cyclomatic Complexity: 1
	 */
	@Test
	@DisplayName("Devolve o tamanho da fragata")
	void testGetSize() {
		assertEquals(4, frigate.getSize(), "Error: Frigate size should be 4.");
	}

	/**
	 * Test for the stillFloating method (all positions intact).
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("Mantém a fragata a flutuar quando nenhuma posição foi atingida")
	void testStillFloating1() {
		assertTrue(frigate.stillFloating(), "Error: Frigate should still be floating.");
	}

	/**
	 * Test for the stillFloating method (one position hit).
	 */
	@Test
	@DisplayName("Mantém a fragata a flutuar depois de atingir uma posição")
	void testStillFloating2() {
		frigate.getPositions().get(0).shoot();
		assertTrue(frigate.stillFloating(), "Error: Frigate should still be floating with one hit position.");
	}

	/**
	 * Test for the stillFloating method (all positions hit).
	 */
	@Test
	@DisplayName("Afunda a fragata depois de atingir todas as posições")
	void testStillFloating3() {
		frigate.getPositions().forEach(IPosition::shoot);
		assertFalse(frigate.stillFloating(), "Error: Frigate should not be floating if all positions are hit.");
	}

	/**
	 * Test for the getTopMostPos method.
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("Devolve a posição mais a Norte")
	void testGetTopMostPos() {
		assertEquals(5, frigate.getTopMostPos(), "Error: The topmost position should be 5.");
	}

	/**
	 * Test for the getBottomMostPos method.
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("Devolve a posição mais a Sul")
	void testGetBottomMostPos() {
		assertEquals(8, frigate.getBottomMostPos(), "Error: The bottommost position should be 8.");
	}

	/**
	 * Test for the getLeftMostPos method.
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("Devolve a posição mais a Oeste")
	void testGetLeftMostPos() {
		assertEquals(5, frigate.getLeftMostPos(), "Error: The leftmost position should be 5.");
	}

	/**
	 * Test for the getRightMostPos method.
	 * Cyclomatic Complexity: 2
	 */
	@Test
	@DisplayName("Devolve a posição mais a Este")
	void testGetRightMostPos() {
		assertEquals(5, frigate.getRightMostPos(), "Error: The rightmost position should be 5.");
	}

	/**
	 * Test for the constructor with invalid input (null).
	 */
	@Test
	@DisplayName("Rejeita parâmetros nulos no construtor")
	void testConstructorInvalidInput() {
		assertThrows(NullPointerException.class, () -> new Frigate(null, null),
				"Error: NullPointerException should be thrown for null input.");
		assertThrows(NullPointerException.class, () -> new Frigate(Compass.NORTH, null),
				"Error: NullPointerException should be thrown for null position.");
	}
}