package restserver;

import battleship.IGame;
import battleship.IMove;
import battleship.IPosition;
import battleship.Position;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for class CallbackClient.
 * Author: Tomás Leal (LEI-111491)
 * Date: 2026-11-20 21:40
 * Cyclomatic Complexity:
 * - CallbackClient(): 1
 * - sendShots(GameSession, List): 9
 * - recordAiMove(IGame, List, ShotResponse): 5
 *
 * O "servidor do aluno" é simulado com um HttpServer local (com.sun.net.httpserver) numa porta livre,
 * que devolve o código HTTP e o corpo configurados em cada teste e guarda o pedido recebido.
 *
 * Ramos não cobríveis sem alterar o código:
 * - o catch da serialização do pedido (o Jackson não falha a serializar um ShotRequest);
 * - statusCode() < 200: o HttpClient do Java trata as respostas 1xx como informativas e nunca as devolve.
 */
class CallbackClientTest {

    private CallbackClient client;
    private HttpServer servidor;
    private final AtomicReference<String> pedidoRecebido = new AtomicReference<>();
    private final AtomicReference<String> caminhoRecebido = new AtomicReference<>();
    private int codigoResposta;
    private String corpoResposta;

    private PrintStream outOriginal;
    private PrintStream errOriginal;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeEach
    void setUp() throws IOException {
        client = new CallbackClient();
        codigoResposta = 200;
        corpoResposta = "{}";
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", troca -> {
            caminhoRecebido.set(troca.getRequestURI().getPath());
            pedidoRecebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = corpoResposta.getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(codigoResposta, bytes.length);
            try (OutputStream os = troca.getResponseBody()) {
                os.write(bytes);
            }
        });
        servidor.start();

        outOriginal = System.out;
        errOriginal = System.err;
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(outOriginal);
        System.setErr(errOriginal);
        servidor.stop(0);
        servidor = null;
        client = null;
    }

    // ---------------------------------------------------------------- auxiliares

    private GameSession sessao() {
        return new GameSession("jogo-1", "Tomas", "http://localhost:" + servidor.getAddress().getPort());
    }

    private static List<IPosition> tiros() {
        return List.of(new Position(0, 0), new Position(1, 4), new Position(9, 9));
    }

    private static String resposta(String resultados, int naviosRestantes, String estado) {
        return "{\"results\": " + resultados + ", \"shipsRemaining\": " + naviosRestantes
                + ", \"gameStatus\": \"" + estado + "\"}";
    }

    private static String resultado(String linha, int coluna, String desfecho) {
        return "{\"row\": \"" + linha + "\", \"column\": " + coluna + ", \"outcome\": \"" + desfecho + "\"}";
    }

    private static String tresResultados() {
        return "[" + resultado("A", 1, "MISS") + ", " + resultado("B", 5, "HIT") + ", " + resultado("J", 10, "SUNK") + "]";
    }

    private static int portaLivreSemServidor() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    private String erros() {
        return err.toString(StandardCharsets.UTF_8);
    }

    private String saida() {
        return out.toString(StandardCharsets.UTF_8);
    }

    // ---------------------------------------------------------------- construtor

    @Test
    @DisplayName("Construtor cria um cliente pronto a usar")
    void callbackClient() {
        assertNotNull(new CallbackClient(), "Error: expected a CallbackClient instance but got null");
    }

    // ---------------------------------------------------------------- sendShots

    @Test
    @DisplayName("sendShots envia os tiros em notação clássica para /game/{gameId}/shots e regista a jogada")
    void sendShots1() {
        corpoResposta = resposta(tresResultados(), 8, "ONGOING");
        GameSession sessao = sessao();
        boolean ok = client.sendShots(sessao, tiros());
        List<IMove> jogadas = sessao.getGame().getAlienMoves();
        assertAll("Envio com sucesso, jogo continua",
                () -> assertTrue(ok, "Error: expected true for a successful callback but got false"),
                () -> assertEquals("/game/jogo-1/shots", caminhoRecebido.get(), "Error: expected path /game/jogo-1/shots but got " + caminhoRecebido.get()),
                () -> assertTrue(pedidoRecebido.get().contains("\"row\":\"A\""), "Error: expected row A in the request but got " + pedidoRecebido.get()),
                () -> assertTrue(pedidoRecebido.get().contains("\"column\":5"), "Error: expected column 5 in the request but got " + pedidoRecebido.get()),
                () -> assertEquals(1, jogadas.size(), "Error: expected 1 recorded AI move but got " + jogadas.size()),
                () -> assertEquals(3, jogadas.get(0).getShotResults().size(), "Error: expected 3 recorded results but got " + jogadas.get(0).getShotResults().size()),
                () -> assertFalse(sessao.isGameOver(), "Error: expected the game to continue but it is over"));
    }

    @Test
    @DisplayName("sendShots devolve false quando o servidor do aluno não responde (falha HTTP)")
    void sendShots2() throws IOException {
        GameSession sessao = new GameSession("jogo-2", "Tomas", "http://localhost:" + portaLivreSemServidor());
        boolean ok = client.sendShots(sessao, tiros());
        assertAll("Falha de ligação",
                () -> assertFalse(ok, "Error: expected false when the student's server is down but got true"),
                () -> assertTrue(erros().contains("HTTP call to student failed"), "Error: expected a connection error message but got '" + erros() + "'"),
                () -> assertTrue(sessao.getGame().getAlienMoves().isEmpty(), "Error: expected no recorded move but got " + sessao.getGame().getAlienMoves().size()));
    }

    @Test
    @DisplayName("sendShots devolve false quando o aluno responde com um código HTTP de erro (>= 300)")
    void sendShots3() {
        codigoResposta = 500;
        corpoResposta = "erro interno";
        boolean ok = client.sendShots(sessao(), tiros());
        assertAll("Código HTTP de erro",
                () -> assertFalse(ok, "Error: expected false for HTTP 500 but got true"),
                () -> assertTrue(erros().contains("Student returned HTTP 500: erro interno"), "Error: expected 'Student returned HTTP 500' but got '" + erros() + "'"));
    }

    @Test
    @DisplayName("sendShots devolve false quando a resposta do aluno não é um JSON válido")
    void sendShots4() {
        corpoResposta = "isto não é JSON";
        boolean ok = client.sendShots(sessao(), tiros());
        assertAll("Resposta inválida",
                () -> assertFalse(ok, "Error: expected false for an invalid m2b response but got true"),
                () -> assertTrue(erros().contains("Failed to parse student m2b response"), "Error: expected a parse error message but got '" + erros() + "'"));
    }

    @Test
    @DisplayName("sendShots marca a vitória da IA quando o aluno fica sem navios")
    void sendShots5() {
        corpoResposta = resposta(tresResultados(), 0, "ONGOING");
        GameSession sessao = sessao();
        boolean ok = client.sendShots(sessao, tiros());
        assertAll("Sem navios restantes",
                () -> assertTrue(ok, "Error: expected true but got false"),
                () -> assertTrue(sessao.isGameOver(), "Error: expected the game to be over"),
                () -> assertEquals("AI_WINS", sessao.getWinner(), "Error: expected winner AI_WINS but got " + sessao.getWinner()),
                () -> assertTrue(saida().contains("GAME OVER — AI wins against 'Tomas'!"), "Error: expected the game over message but got '" + saida() + "'"));
    }

    @Test
    @DisplayName("sendShots marca a vitória da IA quando o estado é GAME_OVER, mesmo com navios restantes")
    void sendShots6() {
        corpoResposta = resposta(tresResultados(), 3, "GAME_OVER");
        GameSession sessao = sessao();
        assertTrue(client.sendShots(sessao, tiros()), "Error: expected true but got false");
        assertEquals("AI_WINS", sessao.getWinner(), "Error: expected winner AI_WINS but got " + sessao.getWinner());
    }

    @Test
    @DisplayName("sendShots aceita códigos de sucesso diferentes de 200 (ex.: 201)")
    void sendShots7() {
        codigoResposta = 201;
        corpoResposta = resposta(tresResultados(), 5, "ONGOING");
        assertTrue(client.sendShots(sessao(), tiros()), "Error: expected true for HTTP 201 but got false");
    }

    // ---------------------------------------------------------------- recordAiMove (privado, testado através de sendShots)

    @Test
    @DisplayName("recordAiMove não regista nada quando a resposta não traz resultados")
    void recordAiMove1() {
        corpoResposta = "{\"shipsRemaining\": 8, \"gameStatus\": \"ONGOING\"}";
        GameSession sessao = sessao();
        assertTrue(client.sendShots(sessao, tiros()), "Error: expected true but got false");
        assertTrue(sessao.getGame().getAlienMoves().isEmpty(),
                "Error: expected no recorded move without results but got " + sessao.getGame().getAlienMoves().size());
    }

    @Test
    @DisplayName("recordAiMove converte INVALID, REPEATED, SUNK e MISS nos campos de ShotResult")
    void recordAiMove2() {
        corpoResposta = resposta("[" + resultado("A", 1, "INVALID") + ", " + resultado("B", 5, "REPEATED") + ", "
                + resultado("J", 10, "SUNK") + "]", 7, "ONGOING");
        GameSession sessao = sessao();
        client.sendShots(sessao, tiros());
        List<IGame.ShotResult> r = sessao.getGame().getAlienMoves().get(0).getShotResults();
        assertAll("Conversão dos desfechos",
                () -> assertFalse(r.get(0).valid(), "Error: expected INVALID to be not valid"),
                () -> assertTrue(r.get(1).valid() && r.get(1).repeated(), "Error: expected REPEATED to be valid and repeated"),
                () -> assertTrue(r.get(2).valid() && r.get(2).sunk() && !r.get(2).repeated(), "Error: expected SUNK to be valid, sunk and not repeated"),
                () -> assertNull(r.get(2).ship(), "Error: expected no ship reference but got " + r.get(2).ship()));
    }

    @Test
    @DisplayName("recordAiMove só regista tantos resultados quantos os tiros, se o aluno devolver resultados a mais")
    void recordAiMove3() {
        corpoResposta = resposta("[" + resultado("A", 1, "MISS") + ", " + resultado("B", 5, "MISS") + ", "
                + resultado("C", 2, "MISS") + ", " + resultado("D", 3, "MISS") + "]", 8, "ONGOING");
        GameSession sessao = sessao();
        client.sendShots(sessao, tiros());
        int registados = sessao.getGame().getAlienMoves().get(0).getShotResults().size();
        assertEquals(3, registados, "Error: expected 3 results (one per shot) but got " + registados);
    }

    @Test
    @DisplayName("recordAiMove só regista os resultados recebidos, se o aluno devolver menos resultados do que tiros")
    void recordAiMove4() {
        corpoResposta = resposta("[" + resultado("A", 1, "HIT") + "]", 8, "ONGOING");
        GameSession sessao = sessao();
        client.sendShots(sessao, tiros());
        List<IMove> jogadas = sessao.getGame().getAlienMoves();
        assertAll("Menos resultados do que tiros",
                () -> assertEquals(1, jogadas.get(0).getShotResults().size(), "Error: expected 1 recorded result but got " + jogadas.get(0).getShotResults().size()),
                () -> assertEquals(3, jogadas.get(0).getShots().size(), "Error: expected the 3 shots to be kept but got " + jogadas.get(0).getShots().size()));
    }

    @Test
    @DisplayName("recordAiMove numera as jogadas da IA de forma sequencial")
    void recordAiMove5() {
        corpoResposta = resposta(tresResultados(), 8, "ONGOING");
        GameSession sessao = sessao();
        client.sendShots(sessao, tiros());
        client.sendShots(sessao, tiros());
        List<IMove> jogadas = sessao.getGame().getAlienMoves();
        assertAll("Numeração das jogadas",
                () -> assertEquals(1, jogadas.get(0).getNumber(), "Error: expected first move number 1 but got " + jogadas.get(0).getNumber()),
                () -> assertEquals(2, jogadas.get(1).getNumber(), "Error: expected second move number 2 but got " + jogadas.get(1).getNumber()));
    }
}
