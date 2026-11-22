package restserver;

import battleship.Game;
import battleship.IGame;
import battleship.IPosition;
import battleship.IShip;
import battleship.Position;
import org.junit.jupiter.api.*;
import org.springframework.http.ResponseEntity;
import restserver.Dtos.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 129844 - Henrique Fernandes
 * Testes unitários de caixa-branca da classe {@link GameController}
 * (Issue "Unit testing task (129844)").
 *
 * subclasses falsas (FakeCallbackClient, FakeAiStrategy) que registam como foram chamadas.
 * O GameRegistry e o GameSession são os verdadeiros. O jogo da IA tem uma frota aleatória, por isso
 * os testes descobrem onde estão os navios em vez de assumir posições fixas.
 */
public class GameControllerTest {

    private GameRegistry registry;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream capturedErr;

    // ------------------------------------------------------------------ fakes

    /** CallbackClient falso: devolve o resultado configurado e regista a chamada. */
    private static class FakeCallbackClient extends CallbackClient {
        private final boolean result;
        int calls = 0;
        GameSession lastSession;
        List<IPosition> lastShots;

        FakeCallbackClient(boolean result) {
            this.result = result;
        }

        @Override
        public boolean sendShots(GameSession session, List<IPosition> shots) {
            calls++;
            lastSession = session;
            lastShots = shots;
            return result;
        }
    }

    /** AiShotStrategy falso: devolve sempre os mesmos 3 tiros e regista a chamada. */
    private static class FakeAiStrategy extends AiShotStrategy {
        final List<IPosition> toReturn = new ArrayList<>(Arrays.asList(
                new Position(0, 0), new Position(0, 1), new Position(0, 2)));
        int calls = 0;
        IGame lastGame;

        @Override
        public List<IPosition> computeShots(IGame game) {
            calls++;
            lastGame = game;
            return toReturn;
        }
    }

    // ------------------------------------------------------------------ fixture

    @BeforeEach
    void setUp() {
        registry = new GameRegistry();
        originalOut = System.out;
        originalErr = System.err;
        capturedErr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(capturedErr, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        registry = null;
    }

    // ------------------------------------------------------------------ helpers

    private GameController controllerWithoutCollaborators() {
        return new GameController(registry, null, null);
    }

    private static int status(ResponseEntity<?> response) {
        return response.getStatusCode().value();
    }

    private static RegistrationRequest registration(String name, String url) {
        RegistrationRequest request = new RegistrationRequest();
        request.playerName = name;
        request.callbackUrl = url;
        return request;
    }

    private static ShotPosition sp(String row, int column) {
        ShotPosition position = new ShotPosition();
        position.row = row;
        position.column = column;
        return position;
    }

    private static ShotPosition sp(IPosition p) {
        return sp(String.valueOf(p.getClassicRow()), p.getClassicColumn());
    }

    private static ShotRequest request(ShotPosition... positions) {
        ShotRequest request = new ShotRequest();
        request.shots = new ArrayList<>(Arrays.asList(positions));
        return request;
    }

    /** Posições sem navio na frota da IA (cópias novas, como num tiro real). */
    private static List<IPosition> waterPositions(IGame game, int howMany) {
        List<IPosition> result = new ArrayList<>();
        for (int r = 0; r < Game.BOARD_SIZE && result.size() < howMany; r++)
            for (int c = 0; c < Game.BOARD_SIZE && result.size() < howMany; c++) {
                IPosition p = new Position(r, c);
                if (game.getMyFleet().shipAt(p) == null)
                    result.add(p);
            }
        assertEquals(howMany, result.size(), "Não há água suficiente no tabuleiro");
        return result;
    }

    private static IPosition copyOf(IPosition p) {
        return new Position(p.getRow(), p.getColumn());
    }

    /** Afunda todos os navios da IA, disparando diretamente nos navios. */
    private static void sinkAllShips(IGame game) {
        for (IShip ship : game.getMyFleet().getShips())
            for (IPosition p : new ArrayList<>(ship.getPositions()))
                ship.shoot(copyOf(p));
        assertEquals(0, game.getRemainingShips(), "Todos os navios deviam estar afundados");
    }

    private GameSession newSession() {
        return registry.createSession("Ana", "http://localhost:9999/callback");
    }

    private static ShotResponse shotBody(ResponseEntity<?> response) {
        assertEquals(200, status(response));
        assertInstanceOf(ShotResponse.class, response.getBody());
        return (ShotResponse) response.getBody();
    }

    // ------------------------------------------------------------------ register

    @Test
    @DisplayName("register: playerName null devolve 400")
    void registerNullPlayerName() {
        ResponseEntity<?> response = controllerWithoutCollaborators()
                .register(registration(null, "http://localhost/cb"));
        assertAll(
                () -> assertEquals(400, status(response)),
                () -> assertInstanceOf(ErrorResponse.class, response.getBody())
        );
    }

    @Test
    @DisplayName("register: playerName em branco devolve 400")
    void registerBlankPlayerName() {
        ResponseEntity<?> response = controllerWithoutCollaborators()
                .register(registration("   ", "http://localhost/cb"));
        assertAll(
                () -> assertEquals(400, status(response)),
                () -> assertInstanceOf(ErrorResponse.class, response.getBody())
        );
    }

    @Test
    @DisplayName("register: callbackUrl null devolve 400")
    void registerNullCallbackUrl() {
        ResponseEntity<?> response = controllerWithoutCollaborators()
                .register(registration("Ana", null));
        assertAll(
                () -> assertEquals(400, status(response)),
                () -> assertInstanceOf(ErrorResponse.class, response.getBody())
        );
    }

    @Test
    @DisplayName("register: callbackUrl em branco devolve 400")
    void registerBlankCallbackUrl() {
        ResponseEntity<?> response = controllerWithoutCollaborators()
                .register(registration("Ana", ""));
        assertAll(
                () -> assertEquals(400, status(response)),
                () -> assertInstanceOf(ErrorResponse.class, response.getBody())
        );
    }

    @Test
    @DisplayName("register: pedido válido devolve 200 e cria a sessão no registo")
    void registerSuccess() {
        ResponseEntity<?> response = controllerWithoutCollaborators()
                .register(registration("Ana", "http://localhost/cb"));
        assertEquals(200, status(response));
        assertInstanceOf(RegistrationResponse.class, response.getBody());
        RegistrationResponse body = (RegistrationResponse) response.getBody();

        GameSession session = registry.getSession(body.gameId);
        assertAll(
                () -> assertNotNull(session, "A sessão deve existir no registo"),
                () -> assertEquals("REGISTERED", body.status),
                () -> assertEquals(session.getShotsPerTurn(), body.shotsPerTurn),
                () -> assertTrue(body.message.contains("Ana"), "A mensagem deve incluir o nome"),
                () -> assertEquals("Ana", session.getPlayerName()),
                () -> assertEquals("http://localhost/cb", session.getCallbackUrl())
        );
    }

    @Test
    @DisplayName("register: dois registos criam jogos diferentes")
    void registerTwiceCreatesDifferentGames() {
        GameController controller = controllerWithoutCollaborators();
        RegistrationResponse first = (RegistrationResponse) controller
                .register(registration("Ana", "http://a")).getBody();
        RegistrationResponse second = (RegistrationResponse) controller
                .register(registration("Rui", "http://b")).getBody();
        assertNotEquals(first.gameId, second.gameId);
    }

    // ------------------------------------------------------------------ receiveShots: sessão

    @Test
    @DisplayName("receiveShots: jogo inexistente devolve 404")
    void receiveShotsUnknownGame() {
        ResponseEntity<?> response = controllerWithoutCollaborators()
                .receiveShots("nao-existe", request(sp("A", 1), sp("A", 2), sp("A", 3)));
        assertAll(
                () -> assertEquals(404, status(response)),
                () -> assertInstanceOf(ErrorResponse.class, response.getBody())
        );
    }

    @Test
    @DisplayName("receiveShots: jogo já terminado devolve 410")
    void receiveShotsGameOver() {
        GameSession session = newSession();
        session.markStudentWins();
        ResponseEntity<?> response = controllerWithoutCollaborators()
                .receiveShots(session.getGameId(), request(sp("A", 1), sp("A", 2), sp("A", 3)));
        assertAll(
                () -> assertEquals(410, status(response)),
                () -> assertInstanceOf(ErrorResponse.class, response.getBody())
        );
    }

    // ------------------------------------------------------------------ receiveShots: validação

    @Test
    @DisplayName("receiveShots: lista de tiros null devolve 400")
    void receiveShotsNullShots() {
        GameSession session = newSession();
        ShotRequest request = new ShotRequest();
        request.shots = null;
        ResponseEntity<?> response = controllerWithoutCollaborators()
                .receiveShots(session.getGameId(), request);
        assertAll(
                () -> assertEquals(400, status(response)),
                () -> assertInstanceOf(ErrorResponse.class, response.getBody())
        );
    }

    @Test
    @DisplayName("receiveShots: número de tiros diferente de 3 devolve 400")
    void receiveShotsWrongNumberOfShots() {
        GameSession session = newSession();
        GameController controller = controllerWithoutCollaborators();
        ResponseEntity<?> few = controller.receiveShots(session.getGameId(),
                request(sp("A", 1), sp("A", 2)));
        ResponseEntity<?> many = controller.receiveShots(session.getGameId(),
                request(sp("A", 1), sp("A", 2), sp("A", 3), sp("A", 4)));
        ResponseEntity<?> none = controller.receiveShots(session.getGameId(), request());
        assertAll(
                () -> assertEquals(400, status(few)),
                () -> assertEquals(400, status(many)),
                () -> assertEquals(400, status(none))
        );
    }

    @Test
    @DisplayName("receiveShots: linha null devolve 400")
    void receiveShotsNullRow() {
        GameSession session = newSession();
        ResponseEntity<?> response = controllerWithoutCollaborators().receiveShots(session.getGameId(),
                request(sp("A", 1), sp(null, 2), sp("A", 3)));
        assertEquals(400, status(response));
    }

    @Test
    @DisplayName("receiveShots: linha em branco devolve 400")
    void receiveShotsBlankRow() {
        GameSession session = newSession();
        ResponseEntity<?> response = controllerWithoutCollaborators().receiveShots(session.getGameId(),
                request(sp("  ", 1), sp("A", 2), sp("A", 3)));
        assertEquals(400, status(response));
    }

    @Test
    @DisplayName("receiveShots: coluna abaixo de 1 devolve 400")
    void receiveShotsColumnTooLow() {
        GameSession session = newSession();
        ResponseEntity<?> response = controllerWithoutCollaborators().receiveShots(session.getGameId(),
                request(sp("A", 1), sp("A", 2), sp("A", 0)));
        assertEquals(400, status(response));
    }

    @Test
    @DisplayName("receiveShots: coluna acima de 10 devolve 400")
    void receiveShotsColumnTooHigh() {
        GameSession session = newSession();
        ResponseEntity<?> response = controllerWithoutCollaborators().receiveShots(session.getGameId(),
                request(sp("A", 11), sp("A", 2), sp("A", 3)));
        assertEquals(400, status(response));
    }

    // ------------------------------------------------------------------ receiveShots: resultados dos tiros

    @Test
    @DisplayName("receiveShots: tiros na água dão MISS, o jogo continua e a IA responde")
    void receiveShotsMiss() {
        GameSession session = newSession();
        IGame game = session.getGame();
        List<IPosition> water = waterPositions(game, 3);
        FakeCallbackClient callback = new FakeCallbackClient(true);
        FakeAiStrategy ai = new FakeAiStrategy();

        ShotResponse body = shotBody(new GameController(registry, callback, ai)
                .receiveShots(session.getGameId(), request(sp(water.get(0)), sp(water.get(1)), sp(water.get(2)))));

        assertAll(
                () -> assertEquals(3, body.results.size()),
                () -> assertEquals("MISS", body.results.get(0).outcome),
                () -> assertEquals("MISS", body.results.get(1).outcome),
                () -> assertEquals("MISS", body.results.get(2).outcome),
                () -> assertEquals("ONGOING", body.gameStatus),
                () -> assertNull(body.winner),
                () -> assertEquals(game.getRemainingShips(), body.shipsRemaining)
        );
    }

    @Test
    @DisplayName("receiveShots: o resultado traz a linha e a coluna clássicas do tiro")
    void receiveShotsEchoesPosition() {
        GameSession session = newSession();
        List<IPosition> water = waterPositions(session.getGame(), 3);
        ShotResponse body = shotBody(new GameController(registry, new FakeCallbackClient(true), new FakeAiStrategy())
                .receiveShots(session.getGameId(), request(sp(water.get(0)), sp(water.get(1)), sp(water.get(2)))));
        IPosition first = water.get(0);
        assertAll(
                () -> assertEquals(String.valueOf(first.getClassicRow()), body.results.get(0).row),
                () -> assertEquals(first.getClassicColumn(), body.results.get(0).column)
        );
    }

    @Test
    @DisplayName("receiveShots: a linha em minúsculas é aceite")
    void receiveShotsLowerCaseRow() {
        GameSession session = newSession();
        List<IPosition> water = waterPositions(session.getGame(), 3);
        ShotPosition lower = sp(String.valueOf(water.get(0).getClassicRow()).toLowerCase(), water.get(0).getClassicColumn());
        ShotResponse body = shotBody(new GameController(registry, new FakeCallbackClient(true), new FakeAiStrategy())
                .receiveShots(session.getGameId(), request(lower, sp(water.get(1)), sp(water.get(2)))));
        assertEquals("MISS", body.results.get(0).outcome);
    }

    @Test
    @DisplayName("receiveShots: acerto num navio que continua a flutuar dá HIT com o tipo de navio")
    void receiveShotsHit() {
        GameSession session = newSession();
        IGame game = session.getGame();
        IShip target = game.getMyFleet().getShips().stream()
                .filter(s -> s.getPositions().size() >= 2).findFirst().orElseThrow();
        IPosition hitPosition = target.getPositions().iterator().next();
        List<IPosition> water = waterPositions(game, 2);
        int remainingBefore = game.getRemainingShips();

        ShotResponse body = shotBody(new GameController(registry, new FakeCallbackClient(true), new FakeAiStrategy())
                .receiveShots(session.getGameId(), request(sp(hitPosition), sp(water.get(0)), sp(water.get(1)))));

        assertAll(
                () -> assertEquals("HIT", body.results.get(0).outcome),
                () -> assertEquals(target.getCategory(), body.results.get(0).shipType),
                () -> assertEquals("MISS", body.results.get(1).outcome),
                () -> assertEquals(remainingBefore, body.shipsRemaining, "Nenhum navio afundou")
        );
    }

    @Test
    @DisplayName("receiveShots: último acerto num navio dá SUNK e reduz os navios restantes")
    void receiveShotsSunk() {
        GameSession session = newSession();
        IGame game = session.getGame();
        IShip target = game.getMyFleet().getShips().iterator().next();
        List<IPosition> cells = new ArrayList<>(target.getPositions());
        // deixa só a última célula por acertar
        for (int i = 0; i < cells.size() - 1; i++)
            target.shoot(copyOf(cells.get(i)));
        IPosition lastCell = cells.get(cells.size() - 1);
        List<IPosition> water = waterPositions(game, 2);
        int remainingBefore = game.getRemainingShips();

        ShotResponse body = shotBody(new GameController(registry, new FakeCallbackClient(true), new FakeAiStrategy())
                .receiveShots(session.getGameId(), request(sp(lastCell), sp(water.get(0)), sp(water.get(1)))));

        assertAll(
                () -> assertEquals("SUNK", body.results.get(0).outcome),
                () -> assertEquals(target.getCategory(), body.results.get(0).shipType),
                () -> assertEquals(remainingBefore - 1, body.shipsRemaining),
                () -> assertEquals("ONGOING", body.gameStatus)
        );
    }

    @Test
    @DisplayName("receiveShots: o mesmo tiro duas vezes na jogada dá REPEATED na segunda")
    void receiveShotsRepeatedInTurn() {
        GameSession session = newSession();
        List<IPosition> water = waterPositions(session.getGame(), 2);
        ShotResponse body = shotBody(new GameController(registry, new FakeCallbackClient(true), new FakeAiStrategy())
                .receiveShots(session.getGameId(), request(sp(water.get(0)), sp(water.get(0)), sp(water.get(1)))));
        assertAll(
                () -> assertEquals("MISS", body.results.get(0).outcome),
                () -> assertEquals("REPEATED", body.results.get(1).outcome),
                () -> assertEquals("MISS", body.results.get(2).outcome)
        );
    }

    @Test
    @DisplayName("receiveShots: linha fora do tabuleiro (K) dá INVALID")
    void receiveShotsInvalidRow() {
        GameSession session = newSession();
        List<IPosition> water = waterPositions(session.getGame(), 2);
        ShotResponse body = shotBody(new GameController(registry, new FakeCallbackClient(true), new FakeAiStrategy())
                .receiveShots(session.getGameId(), request(sp("K", 1), sp(water.get(0)), sp(water.get(1)))));
        assertEquals("INVALID", body.results.get(0).outcome);
    }

    // ------------------------------------------------------------------ receiveShots: resposta da IA

    @Test
    @DisplayName("receiveShots: a IA calcula tiros e o callback recebe a sessão e esses tiros")
    void receiveShotsCallsAiAndCallback() {
        GameSession session = newSession();
        IGame game = session.getGame();
        List<IPosition> water = waterPositions(game, 3);
        FakeCallbackClient callback = new FakeCallbackClient(true);
        FakeAiStrategy ai = new FakeAiStrategy();

        new GameController(registry, callback, ai).receiveShots(session.getGameId(),
                request(sp(water.get(0)), sp(water.get(1)), sp(water.get(2))));

        assertAll(
                () -> assertEquals(1, ai.calls, "A IA calcula tiros uma vez"),
                () -> assertSame(game, ai.lastGame, "A IA recebe o jogo da sessão"),
                () -> assertEquals(1, callback.calls, "O callback é chamado uma vez"),
                () -> assertSame(session, callback.lastSession),
                () -> assertSame(ai.toReturn, callback.lastShots, "O callback recebe os tiros da IA"),
                () -> assertFalse(session.isGameOver(), "O jogo continua")
        );
    }

    @Test
    @DisplayName("receiveShots: se o callback falhar, devolve na mesma 200 e avisa no stderr")
    void receiveShotsCallbackFails() {
        GameSession session = newSession();
        List<IPosition> water = waterPositions(session.getGame(), 3);
        FakeCallbackClient callback = new FakeCallbackClient(false);

        ShotResponse body = shotBody(new GameController(registry, callback, new FakeAiStrategy())
                .receiveShots(session.getGameId(), request(sp(water.get(0)), sp(water.get(1)), sp(water.get(2)))));

        assertAll(
                () -> assertEquals(1, callback.calls),
                () -> assertEquals("ONGOING", body.gameStatus),
                () -> assertTrue(capturedErr.toString(StandardCharsets.UTF_8).contains("WARNING"),
                        "Deve ser escrito um aviso no stderr")
        );
    }

    // ------------------------------------------------------------------ receiveShots: vitória do estudante

    @Test
    @DisplayName("receiveShots: sem navios da IA a flutuar o estudante vence e a IA não responde")
    void receiveShotsStudentWins() {
        GameSession session = newSession();
        IGame game = session.getGame();
        sinkAllShips(game);
        List<IPosition> water = waterPositions(game, 3);
        FakeCallbackClient callback = new FakeCallbackClient(true);
        FakeAiStrategy ai = new FakeAiStrategy();

        ShotResponse body = shotBody(new GameController(registry, callback, ai)
                .receiveShots(session.getGameId(), request(sp(water.get(0)), sp(water.get(1)), sp(water.get(2)))));

        assertAll(
                () -> assertEquals("GAME_OVER", body.gameStatus),
                () -> assertEquals("STUDENT_WINS", body.winner),
                () -> assertEquals(0, body.shipsRemaining),
                () -> assertTrue(session.isGameOver(), "A sessão fica marcada como terminada"),
                () -> assertEquals(0, ai.calls, "A IA não dispara depois de perder"),
                () -> assertEquals(0, callback.calls, "O callback não é chamado")
        );
    }

    @Test
    @DisplayName("receiveShots: depois de o estudante vencer, novos tiros devolvem 410")
    void receiveShotsAfterStudentWins() {
        GameSession session = newSession();
        IGame game = session.getGame();
        sinkAllShips(game);
        List<IPosition> water = waterPositions(game, 3);
        GameController controller = new GameController(registry, new FakeCallbackClient(true), new FakeAiStrategy());
        ShotRequest request = request(sp(water.get(0)), sp(water.get(1)), sp(water.get(2)));

        controller.receiveShots(session.getGameId(), request);
        ResponseEntity<?> second = controller.receiveShots(session.getGameId(), request);

        assertEquals(410, status(second));
    }
}
