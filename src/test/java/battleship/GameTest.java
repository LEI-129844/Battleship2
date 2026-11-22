package battleship;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Game.
 * Author: britoeabreu
 * Date: 2024-03-19
 * Time: 15:30
 * Cyclomatic Complexity for each method:
 * - Game (constructor): 1
 * - fire: 7
 * - getShots: 1
 * - getRepeatedShots: 1
 * - getInvalidShots: 1
 * - getHits: 1
 * - getSunkShips: 1
 * - getRemainingShips: 1
 * - validShot: 3
 * - repeatedShot: 2
 * - printBoard: 1
 * - printValidShots: 1
 * - printFleet: 1
 *
 * Extended by: 129844 - Henrique Fernandes (Issue "Unit testing task (129844)")
 * Objetivo: maximizar a cobertura de linhas e ramos da classe Game.
 * Os testes originais fornecidos foram mantidos no fim do ficheiro.
 *
 * Ramos que NÃO é possível cobrir:
 *  - Game.jsonShots, bloco catch (JsonProcessingException): o ObjectMapper só serializa
 *    Map&lt;String,Object&gt; com Strings e inteiros, que nunca falha, logo a exceção é inalcançável.
 *
 * Comportamentos atuais:
 *  - Se o tabuleiro estiver esgotado (0 posições candidatas), randomEnemyFire gera tiros null e
 *    falha com AssertionError em fireSingleShot (assert pos != null).
 *  - readEnemyFire ignora silenciosamente posições para além das NUMBER_SHOTS primeiras.
 *
 * As asserções "assert x != null" testam-se com assertThrows(AssertionError.class, ...),
 * porque o Maven (e o IntelliJ) correm os testes com -ea.
 */
public class GameTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	/** O ScoreboardManager escreve num ficheiro JSON; fazemos cópia de segurança e repomos no fim. */
	private static final Path[] SCOREBOARD_FILES = {
			Path.of("scoreboard.json"),
			Path.of("data", "scoreboard.json")
	};

	private Game game;

	private final Map<Path, byte[]> scoreboardBackup = new HashMap<>();
	private PrintStream originalOut;
	private ByteArrayOutputStream capturedOut;

	@BeforeEach
	void setUp() throws IOException {
		game = new Game(new Fleet());
		scoreboardBackup.clear();
		for (Path p : SCOREBOARD_FILES)
			if (Files.exists(p))
				scoreboardBackup.put(p, Files.readAllBytes(p));

		originalOut = System.out;
		capturedOut = new ByteArrayOutputStream();
		System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
	}

	@AfterEach
	void tearDown() throws IOException {
		game = null;
		System.setOut(originalOut);
		for (Path p : SCOREBOARD_FILES) {
			if (scoreboardBackup.containsKey(p))
				Files.write(p, scoreboardBackup.get(p));
			else
				Files.deleteIfExists(p);
		}
	}

	// ------------------------------------------------------------------ helpers

	private String output() {
		return capturedOut.toString(StandardCharsets.UTF_8);
	}

	private static Position pos(int row, int col) {
		return new Position(row, col);
	}

	private static List<IPosition> shots(IPosition... positions) {
		return new ArrayList<>(Arrays.asList(positions));
	}

	private static IShip barge(int row, int col) {
		return new Barge(Compass.NORTH, new Position(row, col));
	}

	private static IShip caravel(int row, int col) {
		return new Caravel(Compass.NORTH, new Position(row, col));
	}

	private static Fleet fleetOf(IShip... ships) {
		Fleet fleet = new Fleet();
		for (IShip ship : ships)
			fleet.addShip(ship);
		return fleet;
	}

	/**
	 * Primeira posição do navio, mas como cópia nova: no jogo real os tiros são sempre
	 * posições novas, nunca os objetos internos do navio (que são marcados como atingidos).
	 */
	private static IPosition firstPosition(IShip ship) {
		IPosition p = ship.getPositions().iterator().next();
		return new Position(p.getRow(), p.getColumn());
	}

	private static String coord(IPosition p) {
		return p.getRow() + "," + p.getColumn();
	}

	private static Set<String> coords(Collection<? extends IPosition> positions) {
		Set<String> result = new HashSet<>();
		for (IPosition p : positions)
			result.add(coord(p));
		return result;
	}

	private static List<IPosition> lastShots(Game game) {
		List<IMove> moves = game.getAlienMoves();
		return new ArrayList<>(moves.get(moves.size() - 1).getShots());
	}

	/** Dispara em todas as posições do tabuleiro, exceto as últimas {@code leaveFree} (ordem linha a linha). */
	private static void exhaust(Game game, int leaveFree) {
		int total = Game.BOARD_SIZE * Game.BOARD_SIZE - leaveFree;
		List<IPosition> toFire = new ArrayList<>();
		for (int i = 0; i < total; i++)
			toFire.add(new Position(i / Game.BOARD_SIZE, i % Game.BOARD_SIZE));
		for (int i = 0; i < toFire.size(); i += Game.NUMBER_SHOTS) {
			List<IPosition> chunk = new ArrayList<>(
					toFire.subList(i, Math.min(i + Game.NUMBER_SHOTS, toFire.size())));
			while (chunk.size() < Game.NUMBER_SHOTS)
				chunk.add(chunk.get(0));
			game.fireShots(chunk);
		}
	}

	private static Set<String> freeCoords(int leaveFree) {
		Set<String> result = new HashSet<>();
		for (int i = Game.BOARD_SIZE * Game.BOARD_SIZE - leaveFree; i < Game.BOARD_SIZE * Game.BOARD_SIZE; i++)
			result.add((i / Game.BOARD_SIZE) + "," + (i % Game.BOARD_SIZE));
		return result;
	}

	/** Linhas do tabuleiro impressas (as que terminam em " |"). */
	private static List<String> boardLines(String text) {
		List<String> lines = new ArrayList<>();
		for (String line : text.split("\\R"))
			if (line.endsWith(" |"))
				lines.add(line);
		return lines;
	}

	private static long count(List<String> lines, char ch) {
		return lines.stream().mapToLong(l -> l.chars().filter(c -> c == ch).count()).sum();
	}

	// ------------------------------------------------------------------ construtor e getters

	@Test
	@DisplayName("Construtor: guarda a frota própria e inicia contadores e listas a zero")
	void constructorInitialState() {
		Fleet fleet = new Fleet();
		Game game = new Game(fleet);
		assertAll(
				() -> assertSame(fleet, game.getMyFleet(), "A frota própria deve ser a recebida"),
				() -> assertNotNull(game.getAlienFleet(), "A frota inimiga deve existir"),
				() -> assertNotSame(fleet, game.getAlienFleet(), "A frota inimiga deve ser outra"),
				() -> assertTrue(game.getMyMoves().isEmpty(), "Sem jogadas próprias"),
				() -> assertTrue(game.getAlienMoves().isEmpty(), "Sem jogadas inimigas"),
				() -> assertEquals(0, game.getRepeatedShots()),
				() -> assertEquals(0, game.getInvalidShots()),
				() -> assertEquals(0, game.getHits()),
				() -> assertEquals(0, game.getSunkShips()),
				() -> assertEquals(0, game.getRemainingShips(), "Frota vazia não tem navios")
		);
	}

	@Test
	@DisplayName("Constantes: tabuleiro 10x10 e 3 tiros por jogada")
	void constants() {
		assertAll(
				() -> assertEquals(10, Game.BOARD_SIZE),
				() -> assertEquals(3, Game.NUMBER_SHOTS)
		);
	}

	// ------------------------------------------------------------------ fireSingleShot e repeatedShot

	@Test
	@DisplayName("fireSingleShot: posição null falha a asserção")
	void fireSingleShotNull() {
		Game game = new Game(new Fleet());
		assertThrows(AssertionError.class, () -> game.fireSingleShot(null, false));
	}

	@Test
	@DisplayName("fireSingleShot: posições fora do tabuleiro contam como tiros inválidos")
	void fireSingleShotInvalid() {
		Game game = new Game(new Fleet());
		assertAll(
				() -> assertNotNull(game.fireSingleShot(pos(10, 10), false)),
				() -> assertNotNull(game.fireSingleShot(pos(-1, 0), false)),
				() -> assertEquals(2, game.getInvalidShots(), "Dois tiros inválidos"),
				() -> assertEquals(0, game.getRepeatedShots()),
				() -> assertEquals(0, game.getHits())
		);
	}

	@Test
	@DisplayName("fireSingleShot: isRepeated=true conta como tiro repetido")
	void fireSingleShotMarkedRepeated() {
		Game game = new Game(new Fleet());
		assertNotNull(game.fireSingleShot(pos(2, 2), true));
		assertAll(
				() -> assertEquals(1, game.getRepeatedShots()),
				() -> assertEquals(0, game.getInvalidShots()),
				() -> assertEquals(0, game.getHits())
		);
	}

	@Test
	@DisplayName("fireSingleShot: posição já disparada numa jogada anterior conta como repetida")
	void fireSingleShotRepeatedFromPreviousMove() {
		Game game = new Game(new Fleet());
		IPosition p = pos(3, 3);
		game.fireShots(shots(p, pos(0, 0), pos(0, 1)));
		assertNotNull(game.fireSingleShot(pos(3, 3), false));
		assertEquals(1, game.getRepeatedShots());
	}

	@Test
	@DisplayName("fireSingleShot: tiro na água não conta como acerto")
	void fireSingleShotWater() {
		Game game = new Game(fleetOf(barge(5, 5)));
		assertNotNull(game.fireSingleShot(pos(0, 0), false));
		assertAll(
				() -> assertEquals(0, game.getHits()),
				() -> assertEquals(0, game.getSunkShips()),
				() -> assertEquals(0, game.getRepeatedShots()),
				() -> assertEquals(0, game.getInvalidShots()),
				() -> assertEquals(1, game.getRemainingShips())
		);
	}

	@Test
	@DisplayName("fireSingleShot: acerto num navio que continua a flutuar")
	void fireSingleShotHitNotSunk() {
		IShip caravel = caravel(2, 2);
		Game game = new Game(fleetOf(caravel));
		assertNotNull(game.fireSingleShot(firstPosition(caravel), false));
		assertAll(
				() -> assertEquals(1, game.getHits()),
				() -> assertEquals(0, game.getSunkShips()),
				() -> assertEquals(1, game.getRemainingShips()),
				() -> assertTrue(caravel.stillFloating())
		);
	}

	@Test
	@DisplayName("fireSingleShot: acerto que afunda o navio")
	void fireSingleShotHitAndSunk() {
		IShip barge = barge(5, 5);
		Game game = new Game(fleetOf(barge));
		assertNotNull(game.fireSingleShot(firstPosition(barge), false));
		assertAll(
				() -> assertEquals(1, game.getHits()),
				() -> assertEquals(1, game.getSunkShips()),
				() -> assertEquals(0, game.getRemainingShips()),
				() -> assertFalse(barge.stillFloating())
		);
	}

	@Test
	@DisplayName("fireSingleShot: navio de dois lugares só afunda no segundo acerto")
	void fireSingleShotSinksOnLastHit() {
		IShip caravel = caravel(2, 2);
		Game game = new Game(fleetOf(caravel));
		List<IPosition> positions = new ArrayList<>(caravel.getPositions());
		game.fireSingleShot(positions.get(0), false);
		assertEquals(0, game.getSunkShips(), "Ainda não afundou");
		game.fireSingleShot(positions.get(1), false);
		assertAll(
				() -> assertEquals(2, game.getHits()),
				() -> assertEquals(1, game.getSunkShips()),
				() -> assertEquals(0, game.getRemainingShips())
		);
	}

	@Test
	@DisplayName("repeatedShot: false sem jogadas, true depois de a posição ser disparada")
	void repeatedShot() {
		Game game = new Game(new Fleet());
		IPosition p = pos(4, 4);
		assertFalse(game.repeatedShot(p), "Sem jogadas anteriores");
		game.fireShots(shots(p, pos(0, 0), pos(0, 1)));
		assertAll(
				() -> assertTrue(game.repeatedShot(p), "Já foi disparada"),
				() -> assertFalse(game.repeatedShot(pos(9, 9)), "Nunca foi disparada")
		);
	}

	@Test
	@DisplayName("repeatedShot: posição null falha a asserção")
	void repeatedShotNull() {
		Game game = new Game(new Fleet());
		assertThrows(AssertionError.class, () -> game.repeatedShot(null));
	}

	// ------------------------------------------------------------------ fireShots

	@Test
	@DisplayName("fireShots: lista null falha a asserção")
	void fireShotsNull() {
		Game game = new Game(new Fleet());
		assertThrows(AssertionError.class, () -> game.fireShots(null));
	}

	@Test
	@DisplayName("fireShots: número de tiros diferente de NUMBER_SHOTS lança IllegalArgumentException")
	void fireShotsWrongSize() {
		Game game = new Game(new Fleet());
		IllegalArgumentException few = assertThrows(IllegalArgumentException.class,
				() -> game.fireShots(shots(pos(0, 0), pos(0, 1))));
		IllegalArgumentException many = assertThrows(IllegalArgumentException.class,
				() -> game.fireShots(shots(pos(0, 0), pos(0, 1), pos(0, 2), pos(0, 3))));
		assertAll(
				() -> assertTrue(few.getMessage().contains(String.valueOf(Game.NUMBER_SHOTS))),
				() -> assertNotNull(many.getMessage()),
				() -> assertTrue(game.getAlienMoves().isEmpty(), "Nenhuma jogada deve ser registada")
		);
	}

	@Test
	@DisplayName("fireShots: jogadas válidas são registadas por ordem")
	void fireShotsRegistersMoves() {
		Game game = new Game(new Fleet());
		game.fireShots(shots(pos(0, 0), pos(0, 1), pos(0, 2)));
		assertEquals(1, game.getAlienMoves().size());
		game.fireShots(shots(pos(1, 0), pos(1, 1), pos(1, 2)));
		assertAll(
				() -> assertEquals(2, game.getAlienMoves().size()),
				() -> assertEquals(0, game.getRepeatedShots()),
				() -> assertEquals(0, game.getInvalidShots())
		);
	}

	@Test
	@DisplayName("fireShots: posição repetida na mesma jogada conta como tiro repetido")
	void fireShotsDuplicateInSameMove() {
		Game game = new Game(new Fleet());
		IPosition p = pos(0, 0);
		game.fireShots(shots(p, p, pos(0, 1)));
		assertAll(
				() -> assertEquals(1, game.getRepeatedShots()),
				() -> assertEquals(1, game.getAlienMoves().size())
		);
	}

	@Test
	@DisplayName("fireShots: jogada que afunda um navio atualiza contadores")
	void fireShotsSinksShip() {
		IShip barge = barge(5, 5);
		Game game = new Game(fleetOf(barge));
		game.fireShots(shots(firstPosition(barge), pos(0, 0), pos(10, 10)));
		assertAll(
				() -> assertEquals(1, game.getHits()),
				() -> assertEquals(1, game.getSunkShips()),
				() -> assertEquals(1, game.getInvalidShots()),
				() -> assertEquals(0, game.getRemainingShips()),
				() -> assertEquals(1, game.getAlienMoves().size())
		);
	}

	// ------------------------------------------------------------------ randomEnemyFire

	@Test
	@DisplayName("randomEnemyFire: gera 3 tiros distintos e devolve-os em JSON")
	void randomEnemyFireNormal() throws IOException {
		Game game = new Game(new Fleet());
		String json = game.randomEnemyFire();
		JsonNode node = MAPPER.readTree(json);
		assertAll(
				() -> assertTrue(node.isArray()),
				() -> assertEquals(Game.NUMBER_SHOTS, node.size()),
				() -> assertEquals(1, game.getAlienMoves().size()),
				() -> assertEquals(Game.NUMBER_SHOTS, coords(lastShots(game)).size(), "Tiros distintos")
		);
	}

	@Test
	@DisplayName("randomEnemyFire: nunca repete posições já disparadas em jogadas anteriores")
	void randomEnemyFireNeverRepeats() {
		Game game = new Game(new Fleet());
		Set<String> all = new HashSet<>();
		for (int i = 0; i < 5; i++) {
			game.randomEnemyFire();
			all.addAll(coords(lastShots(game)));
		}
		assertEquals(5 * Game.NUMBER_SHOTS, all.size());
	}

	@Test
	@DisplayName("randomEnemyFire: evita posições adjacentes a navios afundados")
	void randomEnemyFireAvoidsSunkAdjacent() {
		IShip barge = barge(5, 5);
		Game game = new Game(fleetOf(barge));
		game.fireShots(shots(firstPosition(barge), pos(0, 0), pos(0, 1)));
		assertEquals(0, game.getRemainingShips(), "A barca deve estar afundada");

		Set<String> adjacent = coords(barge.getAdjacentPositions());
		assertFalse(adjacent.isEmpty(), "Uma barca afundada tem posições adjacentes");

		List<String> everyShot = new ArrayList<>();
		for (IPosition p : lastShots(game))
			everyShot.add(coord(p));
		for (int i = 0; i < 20; i++) {
			game.randomEnemyFire();
			for (IPosition p : lastShots(game)) {
				String c = coord(p);
				assertFalse(adjacent.contains(c), "Tiro em posição adjacente a navio afundado: " + c);
				everyShot.add(c);
			}
		}

		Set<String> seen = new HashSet<>();
		List<String> repeated = new ArrayList<>();
		for (String c : everyShot)
			if (!seen.add(c))
				repeated.add(c);
		assertAll(
				() -> assertEquals(21 * Game.NUMBER_SHOTS, everyShot.size(), "Número de tiros disparados"),
				() -> assertTrue(repeated.isEmpty(), "Tiros repetidos entre jogadas: " + repeated)
		);
	}

	@Test
	@DisplayName("randomEnemyFire: com exatamente 3 candidatas usa o ramo normal e dispara em todas")
	void randomEnemyFireThreeCandidates() {
		Game game = new Game(new Fleet());
		exhaust(game, 3);
		game.randomEnemyFire();
		assertEquals(freeCoords(3), coords(lastShots(game)));
	}

	@Test
	@DisplayName("randomEnemyFire: com 2 candidatas dispara nas duas e repete uma para completar a rajada")
	void randomEnemyFireTwoCandidates() {
		Game game = new Game(new Fleet());
		exhaust(game, 2);
		String json = game.randomEnemyFire();
		assertAll(
				() -> assertEquals(freeCoords(2), coords(lastShots(game))),
				() -> assertEquals(Game.NUMBER_SHOTS, lastShots(game).size()),
				() -> assertTrue(json.contains("row"))
		);
	}

	@Test
	@DisplayName("randomEnemyFire: com 1 candidata repete-a nos 3 tiros")
	void randomEnemyFireOneCandidate() {
		Game game = new Game(new Fleet());
		exhaust(game, 1);
		game.randomEnemyFire();
		assertAll(
				() -> assertEquals(freeCoords(1), coords(lastShots(game))),
				() -> assertEquals(Game.NUMBER_SHOTS, lastShots(game).size())
		);
	}

	@Test
	@DisplayName("randomEnemyFire: tabuleiro esgotado (comportamento atual) falha com AssertionError")
	void randomEnemyFireNoCandidates() {
		Game game = new Game(new Fleet());
		exhaust(game, 0);
		assertThrows(AssertionError.class, game::randomEnemyFire);
	}

	// ------------------------------------------------------------------ readEnemyFire

	@Test
	@DisplayName("readEnemyFire: scanner null falha a asserção")
	void readEnemyFireNullScanner() {
		Game game = new Game(new Fleet());
		assertThrows(AssertionError.class, () -> game.readEnemyFire(null));
	}

	@Test
	@DisplayName("readEnemyFire: formato 'A1 B2 C3' regista uma jogada e devolve JSON com 3 tiros")
	void readEnemyFireCombinedTokens() throws IOException {
		Game game = new Game(new Fleet());
		String json = game.readEnemyFire(new Scanner("A1 B2 C3\n"));
		JsonNode node = MAPPER.readTree(json);
		assertAll(
				() -> assertEquals(Game.NUMBER_SHOTS, node.size()),
				() -> assertEquals(1, game.getAlienMoves().size())
		);
	}

	@Test
	@DisplayName("readEnemyFire: 'A 1 B 2 C 3' (letra e número separados) equivale a 'A1 B2 C3'")
	void readEnemyFireSeparateTokens() {
		String combined = new Game(new Fleet()).readEnemyFire(new Scanner("A1 B2 C3\n"));
		String separate = new Game(new Fleet()).readEnemyFire(new Scanner("A 1 B 2 C 3\n"));
		assertEquals(combined, separate);
	}

	@Test
	@DisplayName("readEnemyFire: letras minúsculas são convertidas para maiúsculas")
	void readEnemyFireLowerCase() {
		String upper = new Game(new Fleet()).readEnemyFire(new Scanner("A 1 B 2 C 3\n"));
		String lower = new Game(new Fleet()).readEnemyFire(new Scanner("a 1 b 2 c 3\n"));
		assertEquals(upper, lower);
	}

	@Test
	@DisplayName("readEnemyFire: mistura de tokens juntos e separados")
	void readEnemyFireMixedTokens() {
		String expected = new Game(new Fleet()).readEnemyFire(new Scanner("A1 B2 C3\n"));
		String mixed = new Game(new Fleet()).readEnemyFire(new Scanner("A1 B 2 C3\n"));
		assertEquals(expected, mixed);
	}

	@Test
	@DisplayName("readEnemyFire: letra não seguida de número lança IllegalArgumentException")
	void readEnemyFireIncompletePosition() {
		Game game = new Game(new Fleet());
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> game.readEnemyFire(new Scanner("A B 2 C 3\n")));
		assertAll(
				() -> assertTrue(ex.getMessage().contains("incompleta")),
				() -> assertTrue(game.getAlienMoves().isEmpty())
		);
	}

	@Test
	@DisplayName("readEnemyFire: letra no fim da linha também é posição incompleta")
	void readEnemyFireLetterAtEnd() {
		Game game = new Game(new Fleet());
		assertThrows(IllegalArgumentException.class,
				() -> game.readEnemyFire(new Scanner("A1 B2 C\n")));
	}

	@Test
	@DisplayName("readEnemyFire: menos de 3 posições lança IllegalArgumentException")
	void readEnemyFireTooFew() {
		Game game = new Game(new Fleet());
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> game.readEnemyFire(new Scanner("A1 B2\n")));
		assertAll(
				() -> assertTrue(ex.getMessage().contains("exatamente")),
				() -> assertTrue(game.getAlienMoves().isEmpty())
		);
	}

	@Test
	@DisplayName("readEnemyFire: linha vazia lança IllegalArgumentException")
	void readEnemyFireEmptyLine() {
		Game game = new Game(new Fleet());
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(new Scanner("\n")));
	}

	@Test
	@DisplayName("readEnemyFire: posições a mais são ignoradas (comportamento atual)")
	void readEnemyFireIgnoresExtra() {
		Game game = new Game(new Fleet());
		String json = game.readEnemyFire(new Scanner("A1 B2 C3 D4\n"));
		assertAll(
				() -> assertEquals(1, game.getAlienMoves().size()),
				() -> assertEquals(Game.NUMBER_SHOTS, lastShots(game).size()),
				() -> assertNotNull(json)
		);
	}

	@Test
	@DisplayName("readEnemyFire: scanner sem linha lança NoSuchElementException")
	void readEnemyFireNoInput() {
		Game game = new Game(new Fleet());
		assertThrows(NoSuchElementException.class, () -> game.readEnemyFire(new Scanner("")));
	}

	// ------------------------------------------------------------------ jsonShots

	@Test
	@DisplayName("jsonShots: lista null falha a asserção")
	void jsonShotsNull() {
		assertThrows(AssertionError.class, () -> Game.jsonShots(null));
	}

	@Test
	@DisplayName("jsonShots: lista vazia devolve um array JSON vazio")
	void jsonShotsEmpty() throws IOException {
		JsonNode node = MAPPER.readTree(Game.jsonShots(new ArrayList<>()));
		assertAll(
				() -> assertTrue(node.isArray()),
				() -> assertEquals(0, node.size())
		);
	}

	@Test
	@DisplayName("jsonShots: cada tiro tem a linha e a coluna clássicas")
	void jsonShotsContent() throws IOException {
		List<IPosition> list = shots(pos(0, 0), pos(1, 2), pos(9, 9));
		String json = Game.jsonShots(list);
		JsonNode node = MAPPER.readTree(json);
		assertEquals(3, node.size());
		for (int i = 0; i < list.size(); i++) {
			IPosition p = list.get(i);
			JsonNode item = node.get(i);
			assertAll("tiro " + i,
					() -> assertEquals(String.valueOf(p.getClassicRow()), item.get("row").asText()),
					() -> assertEquals(p.getClassicColumn(), item.get("column").asInt())
			);
		}
		assertTrue(output().contains("row"), "O JSON também é impresso na consola");
	}

	// ------------------------------------------------------------------ printBoard e variantes

	@Test
	@DisplayName("printBoard: frota ou lista de jogadas null falham a asserção")
	void printBoardNulls() {
		assertAll(
				() -> assertThrows(AssertionError.class,
						() -> Game.printBoard(null, new ArrayList<IMove>(), false, false)),
				() -> assertThrows(AssertionError.class,
						() -> Game.printBoard(new Fleet(), null, false, false))
		);
	}

	@Test
	@DisplayName("printBoard: tabuleiro vazio, sem tiros e sem legenda")
	void printBoardEmpty() {
		Game.printBoard(new Fleet(), new ArrayList<IMove>(), false, false);
		String out = output();
		List<String> lines = boardLines(out);
		assertAll(
				() -> assertEquals(Game.BOARD_SIZE, lines.size(), "10 linhas de tabuleiro"),
				() -> assertEquals(Game.BOARD_SIZE * Game.BOARD_SIZE, count(lines, '.'), "100 células vazias"),
				() -> assertFalse(out.contains("LEGENDA"), "Sem legenda")
		);
	}

	@Test
	@DisplayName("printBoard: com legenda imprime a explicação dos símbolos")
	void printBoardWithLegend() {
		Game.printBoard(new Fleet(), new ArrayList<IMove>(), false, true);
		String out = output();
		assertAll(
				() -> assertTrue(out.contains("LEGENDA")),
				() -> assertTrue(out.contains("'#'->navio")),
				() -> assertEquals(Game.BOARD_SIZE, boardLines(out).size())
		);
	}

	@Test
	@DisplayName("printBoard: navio a flutuar aparece como '#' sem marcas de adjacência")
	void printBoardFloatingShip() {
		Game.printBoard(fleetOf(barge(5, 5)), new ArrayList<IMove>(), false, false);
		List<String> lines = boardLines(output());
		assertAll(
				() -> assertEquals(1, count(lines, '#')),
				() -> assertEquals(0, count(lines, '-'), "Navio a flutuar não marca adjacentes")
		);
	}

	@Test
	@DisplayName("printBoard: navio afundado marca as posições adjacentes com '-'")
	void printBoardSunkShip() {
		IShip barge = barge(5, 5);
		barge.shoot(firstPosition(barge));
		Game.printBoard(fleetOf(barge), new ArrayList<IMove>(), false, false);
		List<String> lines = boardLines(output());
		assertAll(
				() -> assertEquals(1, count(lines, '#')),
				() -> assertTrue(count(lines, '-') > 0, "Devem existir marcas de adjacência")
		);
	}

	@Test
	@DisplayName("printBoard: mostra tiros certeiros '*', tiros na água 'o' e ignora tiros fora do tabuleiro")
	void printBoardShowShots() {
		IShip barge = barge(5, 5);
		Fleet fleet = fleetOf(barge);
		Game game = new Game(fleet);
		// 1.ª jogada: acerta (afunda), água, fora do tabuleiro
		game.fireShots(shots(firstPosition(barge), pos(0, 0), pos(-1, -1)));
		// 2.ª jogada: posição adjacente ao navio afundado e duas na água
		game.fireShots(shots(pos(5, 6), pos(9, 9), pos(9, 8)));

		Game.printBoard(fleet, game.getAlienMoves(), true, false);
		List<String> lines = boardLines(output());
		assertAll(
				() -> assertEquals(1, count(lines, '*'), "Um tiro certeiro"),
				() -> assertEquals(0, count(lines, '#'), "O navio foi atingido"),
				() -> assertEquals(4, count(lines, 'o'), "Quatro tiros na água (inclui o adjacente)"),
				() -> assertTrue(count(lines, '-') > 0, "Restam marcas de adjacência")
		);
	}

	@Test
	@DisplayName("printBoard: com show_shots=false os tiros não aparecem")
	void printBoardHideShots() {
		IShip barge = barge(5, 5);
		Fleet fleet = fleetOf(barge);
		Game game = new Game(fleet);
		game.fireShots(shots(pos(0, 0), pos(0, 1), pos(0, 2)));

		Game.printBoard(fleet, game.getAlienMoves(), false, false);
		List<String> lines = boardLines(output());
		assertAll(
				() -> assertEquals(0, count(lines, 'o')),
				() -> assertEquals(0, count(lines, '*')),
				() -> assertEquals(1, count(lines, '#'))
		);
	}

	@Test
	@DisplayName("printMyBoard: imprime a frota própria com os tiros inimigos")
	void printMyBoard() {
		IShip barge = barge(5, 5);
		Game game = new Game(fleetOf(barge));
		game.fireShots(shots(firstPosition(barge), pos(0, 0), pos(0, 1)));
		game.printMyBoard(true, true);
		String out = output();
		List<String> lines = boardLines(out);
		assertAll(
				() -> assertTrue(out.contains("LEGENDA")),
				() -> assertEquals(1, count(lines, '*')),
				() -> assertEquals(2, count(lines, 'o'))
		);
	}

	@Test
	@DisplayName("printAlienBoard: imprime a frota inimiga (vazia) sem legenda")
	void printAlienBoard() {
		Game game = new Game(new Fleet());
		game.printAlienBoard(true, false);
		String out = output();
		List<String> lines = boardLines(out);
		assertAll(
				() -> assertEquals(Game.BOARD_SIZE, lines.size()),
				() -> assertEquals(Game.BOARD_SIZE * Game.BOARD_SIZE, count(lines, '.')),
				() -> assertFalse(out.contains("LEGENDA"))
		);
	}

	// ------------------------------------------------------------------ over

	@Test
	@DisplayName("over: sem navios a flutuar regista vitória e imprime a mensagem final")
	void overNoRemainingShips() {
		Game game = new Game(new Fleet());
		assertDoesNotThrow(game::over);
		assertTrue(output().contains("Maldito sejas"));
	}

	@Test
	@DisplayName("over: com navios a flutuar regista derrota")
	void overWithRemainingShips() {
		Game game = new Game(fleetOf(barge(5, 5)));
		game.fireShots(shots(pos(0, 0), pos(0, 1), pos(0, 2)));
		assertEquals(1, game.getRemainingShips());
		assertDoesNotThrow(game::over);
		assertTrue(output().contains("Maldito sejas"));
	}

	// ------------------------------------------------------------------ testes originais (mantidos)

	@Test
	void constructor() {
		assertNotNull(game, "Game instance should not be null after construction.");
		assertNotNull(game.getAlienMoves(), "Shots list should not be null after initialization.");
		assertTrue(game.getAlienMoves().isEmpty(), "Shots list should be empty upon initialization.");
		assertEquals(0, game.getInvalidShots(), "Invalid shots count should be zero upon initialization.");
		assertEquals(0, game.getRepeatedShots(), "Repeated shots count should be zero upon initialization.");
		assertEquals(0, game.getHits(), "Hits count should be zero upon initialization.");
		assertEquals(0, game.getSunkShips(), "Sunk ships count should be zero upon initialization.");
	}

	@Test
	void fire2() {
		Position invalidPosition = new Position(-1, 5);
		game.fireSingleShot(invalidPosition, false);
		assertEquals(1, game.getInvalidShots(), "Invalid shots counter should increase for an invalid shot.");
	}

	@Test
	void fire3() {
		Position position = new Position(2, 3);
		game.fireSingleShot(position, false);
		game.fireSingleShot(position, true);
		assertEquals(1, game.getRepeatedShots(), "Repeated shots counter should increase for a repeated shot.");
	}

	@Test
	void repeatedShot1() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		Position position = new Position(2, 3);
		assertTrue(game.repeatedShot(position), "Position (2,3) should be marked as repeated after firing.");
	}

	@Test
	void repeatedShot2() {
		Position position = new Position(2, 3);
		assertFalse(game.repeatedShot(position), "Position (2,3) should not be marked as repeated before firing.");
	}

	@Test
	void getAlienMoves() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		assertEquals(1, game.getAlienMoves().size(), "Shots list should contain one shot after firing once.");
	}

	@Test
	void getRemainingShips() {
		IFleet fleet = game.getMyFleet();
		Ship ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		Ship ship2 = new Frigate(Compass.EAST, new Position(5, 5));

		fleet.addShip(ship1);
		assertEquals(1, game.getRemainingShips(), "Just one ship was created!");
		fleet.addShip(ship2);
		assertEquals(2, game.getRemainingShips(), "Two ships were created!");
		ship2.sink();
		assertEquals(1, game.getRemainingShips(), "Remaining ships count should be 1 after sinking one of two ships.");
	}
}