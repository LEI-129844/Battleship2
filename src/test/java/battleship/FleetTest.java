package battleship;

import org.junit.jupiter.api.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;

/**
 * Test class for Fleet.
 * Author: ${user.name} (testes adicionais: Tomás Leal, LEI-111491)
 * Date: ${current_date} (testes adicionais: 2026-11-20)
 * Time: ${current_time}
 * Cyclomatic Complexity for each method:
 * - Constructor: 1
 * - createRandom: 3
 * - addShip: 4
 * - getShips: 1
 * - getShipsLike: 3
 * - getFloatingShips: 2
 * - getSunkShips: 2
 * - shipAt: 3
 * - colisionRisk: 3
 * - printShips: 2
 * - printStatus: 1
 * - printShipsByCategory: 1
 * - printFloatingShips: 1
 * - printAllShips: 1
 */
	public class FleetTest {

		private Fleet fleet;

		@BeforeEach
		void setUp() {
			fleet = new Fleet();
		}

		@AfterEach
		void tearDown() {
			fleet = null;
		}

		/**
		 * Test for the Fleet constructor.
		 * Cyclomatic Complexity: 1
		 */
		@Test
		@DisplayName("Construtor cria uma frota vazia")
		void testConstructor() {
			assertNotNull(fleet, "Error: Instance of Fleet should not be null.");
			assertTrue(fleet.getShips().isEmpty(), "Error: Fleet should be initialized with empty ships list.");
		}

		@Test
		@DisplayName("createRandom cria uma frota completa com FLEET_SIZE navios")
		void testCreateRandom(){
				IFleet r = Fleet.createRandom();
				assertNotNull(r, "Error: createRandom should return a non-null fleet instance.");
				assertEquals(Fleet.FLEET_SIZE, r.getShips().size(), "Error: createRandom should create a fleet with the correct number of ships.");
		}

		/**
		 * Test for the addShip method (all conditions true).
		 * Cyclomatic Complexity: 3
		 */
		@Test
		@DisplayName("addShip aceita um navio válido dentro do tabuleiro e sem colisões")
		void testAddShip1() {
			IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
			assertTrue(fleet.addShip(ship), "Error: Valid ship should be added successfully.");
			assertEquals(1, fleet.getShips().size(), "Error: Fleet should contain one ship after addition.");
		}

		/**
		 * Test for the addShip method (fleet size limit reached).
		 */
		@Test
		@DisplayName("addShip rejeita um navio encostado aos anteriores (frota preenchida em linhas contíguas)")
		void testAddShip2() {
			for (int i = 0; i < Fleet.FLEET_SIZE; i++) {
				fleet.addShip(new Barge(Compass.NORTH, new Position(i, 0)));
			}
			IShip anotherShip = new Barge(Compass.NORTH, new Position(10, 10));
			assertFalse(fleet.addShip(anotherShip), "Error: Should not add ship when fleet size limit is reached.");
		}

		/**
		 * Test for the addShip method (ship outside the board).
		 */
		@Test
		@DisplayName("addShip rejeita um navio fora do tabuleiro")
		void testAddShip3() {
			IShip shipOutside = new Barge(Compass.NORTH, new Position(99, 99));
			assertFalse(fleet.addShip(shipOutside), "Error: Should not add ship outside the board.");
		}

		/**
		 * Test for the addShip method (collision risk).
		 */
		@Test
		@DisplayName("addShip rejeita um navio sobreposto a outro (risco de colisão)")
		void testAddShip4() {
			IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
			IShip ship2 = new Barge(Compass.NORTH, new Position(1, 1));  // Overlapping position
			fleet.addShip(ship1);
			assertFalse(fleet.addShip(ship2), "Error: Should not add ship with a collision risk.");
		}

		/**
		 * Test for the getShips method.
		 * Cyclomatic Complexity: 1
		 */
		@Test
		@DisplayName("getShips devolve os navios adicionados")
		void testGetShips() {
			assertTrue(fleet.getShips().isEmpty(), "Error: Fleet's ships list should initially be empty.");
			IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
			fleet.addShip(ship);
			assertEquals(1, fleet.getShips().size(), "Error: Fleet should have size 1 after adding a ship.");
			assertEquals(ship, fleet.getShips().get(0), "Error: Fleet's first ship should match the added ship.");
		}

		/**
		 * Test for the getShipsLike method (ships of specific category).
		 * Cyclomatic Complexity: 2
		 */
		@Test
		@DisplayName("getShipsLike devolve só os navios da categoria pedida")
		void testGetShipsLike() {
			IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
			IShip ship2 = new Caravel(Compass.NORTH, new Position(2, 1));
			fleet.addShip(ship1);
			fleet.addShip(ship2);

			List<IShip> barges = fleet.getShipsLike("Barca");
			assertEquals(1, barges.size(), "Error: There should be exactly one ship of category 'Barca'.");
			assertEquals(ship1, barges.get(0), "Error: The ship of category 'Barca' does not match.");
		}

		/**
		 * Test for the getFloatingShips method.
		 * Cyclomatic Complexity: 2
		 */
		@Test
		@DisplayName("getFloatingShips exclui os navios afundados")
		void testGetFloatingShips() {
			IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
			IShip ship2 = new Caravel(Compass.NORTH, new Position(4, 4));
			fleet.addShip(ship1);
			fleet.addShip(ship2);

			List<IShip> floatingShips = fleet.getFloatingShips();
			assertEquals(2, floatingShips.size(), "Error: All ships should be floating initially.");

			ship1.getPositions().get(0).shoot();  // Sink ship1
			floatingShips = fleet.getFloatingShips();
			assertEquals(1, floatingShips.size(), "Error: Only one ship should be floating after sinking one.");
			assertEquals(ship2, floatingShips.get(0), "Error: The floating ship should match the expected result.");
		}

		/**
		 * Test for the shipAt method.
		 * Cyclomatic Complexity: 2
		 */
		@Test
		@DisplayName("shipAt devolve o navio na posição ou null se estiver vazia")
		void testShipAt() {
			IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
			fleet.addShip(ship);

			assertEquals(ship, fleet.shipAt(new Position(1, 1)), "Error: Should return the correct ship at the position.");
			assertNull(fleet.shipAt(new Position(5, 5)), "Error: Should return null for empty positions in the fleet.");
		}


		/**
		 * Test for private method colisionRisk.
		 * Cyclomatic Complexity: 2
		 */
		@Test
		@DisplayName("colisionRisk deteta navios demasiado próximos")
		void testColisionRisk() throws Exception {
			var method = Fleet.class.getDeclaredMethod("colisionRisk", IShip.class);
			method.setAccessible(true);

			IShip ship1 = new Barge(Compass.NORTH, new Position(1, 1));
			IShip ship2 = new Barge(Compass.NORTH, new Position(1, 1));  // Overlapping position
			fleet.addShip(ship1);

			assertTrue((Boolean) method.invoke(fleet, ship2), "Error: Overlapping ships should be at collision risk.");
			assertFalse((Boolean) method.invoke(fleet, new Barge(Compass.NORTH, new Position(5, 5))),
					"Error: Ships at non-overlapping positions should not have a collision risk.");
		}

		/**
		 * Test for the printStatus method.
		 * Cyclomatic Complexity: 1
		 */
		@Test
		@DisplayName("printStatus não lança exceções")
		void testPrintStatus() {
			IShip ship = new Barge(Compass.NORTH, new Position(1, 1));
			fleet.addShip(ship);
			assertDoesNotThrow(fleet::printStatus, "Error: printStatus should not throw any exceptions.");
		}

		// ---------------------------------------------------------------
		// Testes adicionais (LEI-111491) para atingir 100% de linhas e ramos
		// ---------------------------------------------------------------

		/** Captura o que é escrito na consola durante a execução de um bloco. */
		private String capturarSaida(Runnable acao) {
			PrintStream original = System.out;
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
			try {
				acao.run();
			} finally {
				System.setOut(original);
			}
			return buffer.toString(StandardCharsets.UTF_8);
		}

		/** Adiciona n barcas em posições afastadas (linhas e colunas pares), sem colisões. */
		private void adicionarBarcasAfastadas(int n) {
			int adicionadas = 0;
			for (int r = 0; r < 10 && adicionadas < n; r += 2)
				for (int c = 0; c < 10 && adicionadas < n; c += 2)
					if (fleet.addShip(new Barge(Compass.NORTH, new Position(r, c))))
						adicionadas++;
		}

		@Test
		@DisplayName("addShip rejeita um navio quando a frota já ultrapassou FLEET_SIZE navios")
		void testAddShip5() {
			adicionarBarcasAfastadas(Fleet.FLEET_SIZE + 1);
			assertEquals(Fleet.FLEET_SIZE + 1, fleet.getShips().size(),
					"Error: expected " + (Fleet.FLEET_SIZE + 1) + " ships (the condition is size <= FLEET_SIZE) but got " + fleet.getShips().size());
			IShip extra = new Barge(Compass.NORTH, new Position(9, 9));
			assertFalse(fleet.addShip(extra), "Error: expected the ship to be rejected because the fleet is full, but it was added");
		}

		@Test
		@DisplayName("addShip com null falha a asserção (AssertionError)")
		void testAddShip6() {
			assertThrows(AssertionError.class, () -> fleet.addShip(null),
					"Error: expected an AssertionError for a null ship but none was thrown");
		}

		@Test
		@DisplayName("getShipsLike devolve uma lista vazia quando nenhum navio é da categoria pedida")
		void testGetShipsLike2() {
			fleet.addShip(new Barge(Compass.NORTH, new Position(1, 1)));
			assertTrue(fleet.getShipsLike("Galeao").isEmpty(),
					"Error: expected no ships of category 'Galeao' but got " + fleet.getShipsLike("Galeao").size());
		}

		@Test
		@DisplayName("getShipsLike com categoria null falha a asserção (AssertionError)")
		void testGetShipsLike3() {
			assertThrows(AssertionError.class, () -> fleet.getShipsLike(null),
					"Error: expected an AssertionError for a null category but none was thrown");
		}

		@Test
		@DisplayName("getSunkShips devolve só os navios afundados")
		void testGetSunkShips() {
			IShip barca = new Barge(Compass.NORTH, new Position(1, 1));
			IShip caravela = new Caravel(Compass.NORTH, new Position(4, 4));
			fleet.addShip(barca);
			fleet.addShip(caravela);
			assertTrue(fleet.getSunkShips().isEmpty(), "Error: expected no sunk ships initially but got " + fleet.getSunkShips().size());
			barca.getPositions().get(0).shoot();
			List<IShip> afundados = fleet.getSunkShips();
			assertAll("Navios afundados",
					() -> assertEquals(1, afundados.size(), "Error: expected 1 sunk ship but got " + afundados.size()),
					() -> assertSame(barca, afundados.get(0), "Error: expected the barge to be the sunk ship"));
		}

		@Test
		@DisplayName("shipAt com posição null falha a asserção (AssertionError)")
		void testShipAt2() {
			assertThrows(AssertionError.class, () -> fleet.shipAt(null),
					"Error: expected an AssertionError for a null position but none was thrown");
		}

		@Test
		@DisplayName("colisionRisk com navio null falha a asserção (AssertionError)")
		void testColisionRisk2() throws Exception {
			var method = Fleet.class.getDeclaredMethod("colisionRisk", IShip.class);
			method.setAccessible(true);
			var excecao = assertThrows(java.lang.reflect.InvocationTargetException.class, () -> method.invoke(fleet, (Object) null),
					"Error: expected the invocation to fail for a null ship but it did not");
			assertInstanceOf(AssertionError.class, excecao.getCause(),
					"Error: expected an AssertionError as the cause but got " + excecao.getCause());
		}

		@Test
		@DisplayName("printShips escreve cada navio da lista numa linha")
		void testPrintShips1() {
			IShip barca = new Barge(Compass.NORTH, new Position(1, 1));
			IShip caravela = new Caravel(Compass.NORTH, new Position(4, 4));
			String saida = capturarSaida(() -> fleet.printShips(List.of(barca, caravela)));
			assertAll("Navios impressos",
					() -> assertTrue(saida.contains(barca.toString()), "Error: expected the barge in the output but got '" + saida + "'"),
					() -> assertTrue(saida.contains(caravela.toString()), "Error: expected the caravel in the output but got '" + saida + "'"));
		}

		@Test
		@DisplayName("printShips com lista vazia não escreve nada")
		void testPrintShips2() {
			String saida = capturarSaida(() -> fleet.printShips(new ArrayList<>()));
			assertEquals("", saida, "Error: expected no output for an empty list but got '" + saida + "'");
		}

		@Test
		@DisplayName("printShips com lista null falha a asserção (AssertionError)")
		void testPrintShips3() {
			assertThrows(AssertionError.class, () -> fleet.printShips(null),
					"Error: expected an AssertionError for a null list but none was thrown");
		}

		@Test
		@DisplayName("printStatus mostra quantos navios estão a flutuar e quantos foram afundados")
		void testPrintStatus2() {
			IShip barca = new Barge(Compass.NORTH, new Position(1, 1));
			fleet.addShip(barca);
			fleet.addShip(new Caravel(Compass.NORTH, new Position(4, 4)));
			barca.getPositions().get(0).shoot();
			String saida = capturarSaida(fleet::printStatus);
			assertTrue(saida.contains("Estado da Frota: 1 a flutuar, 1 afundados!"),
					"Error: expected 'Estado da Frota: 1 a flutuar, 1 afundados!' but got '" + saida + "'");
		}

		@Test
		@DisplayName("printShipsByCategory escreve só os navios da categoria pedida")
		void testPrintShipsByCategory() {
			IShip barca = new Barge(Compass.NORTH, new Position(1, 1));
			IShip caravela = new Caravel(Compass.NORTH, new Position(4, 4));
			fleet.addShip(barca);
			fleet.addShip(caravela);
			String saida = capturarSaida(() -> fleet.printShipsByCategory("Barca"));
			assertAll("Navios da categoria Barca",
					() -> assertTrue(saida.contains(barca.toString()), "Error: expected the barge in the output but got '" + saida + "'"),
					() -> assertFalse(saida.contains(caravela.toString()), "Error: expected no caravel in the output but got '" + saida + "'"));
		}

		@Test
		@DisplayName("printShipsByCategory com categoria null falha a asserção (AssertionError)")
		void testPrintShipsByCategory2() {
			assertThrows(AssertionError.class, () -> fleet.printShipsByCategory(null),
					"Error: expected an AssertionError for a null category but none was thrown");
		}

		@Test
		@DisplayName("printFloatingShips escreve só os navios que ainda flutuam")
		void testPrintFloatingShips() {
			IShip barca = new Barge(Compass.NORTH, new Position(1, 1));
			IShip caravela = new Caravel(Compass.NORTH, new Position(4, 4));
			fleet.addShip(barca);
			fleet.addShip(caravela);
			barca.getPositions().get(0).shoot();
			String saida = capturarSaida(fleet::printFloatingShips);
			assertAll("Navios a flutuar",
					() -> assertFalse(saida.contains(barca.toString()), "Error: expected no sunk barge in the output but got '" + saida + "'"),
					() -> assertTrue(saida.contains(caravela.toString()), "Error: expected the caravel in the output but got '" + saida + "'"));
		}

		@Test
		@DisplayName("printAllShips escreve todos os navios da frota, afundados ou não")
		void testPrintAllShips() {
			IShip barca = new Barge(Compass.NORTH, new Position(1, 1));
			IShip caravela = new Caravel(Compass.NORTH, new Position(4, 4));
			fleet.addShip(barca);
			fleet.addShip(caravela);
			barca.getPositions().get(0).shoot();
			String saida = capturarSaida(fleet::printAllShips);
			assertAll("Todos os navios",
					() -> assertTrue(saida.contains(barca.toString()), "Error: expected the barge in the output but got '" + saida + "'"),
					() -> assertTrue(saida.contains(caravela.toString()), "Error: expected the caravel in the output but got '" + saida + "'"));
		}
	}
