package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for class ScoreboardManager.
 * Author: Tomás Leal (LEI-111491)
 * Date: 2026-11-20 18:00
 * Cyclomatic Complexity:
 * - ScoreboardManager() (construtor implícito): 1
 * - carregarScores(): 4
 * - registarPartida(int, int, boolean): 2
 * - imprimirScoreboard(): 3
 *
 * O ScoreboardManager usa sempre o ficheiro "scoreboard.json" na pasta de trabalho.
 * Antes de cada teste o ficheiro existente (se houver) é guardado à parte e no fim é reposto,
 * para os testes não apagarem o histórico real do jogo.
 */
class ScoreboardManagerTest {

    private static final Path FICHEIRO = Paths.get("scoreboard.json");
    private static final Path COPIA = Paths.get("scoreboard.json.bak-teste");

    private PrintStream outOriginal;
    private PrintStream errOriginal;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;

    @BeforeEach
    void setUp() throws IOException {
        if (Files.exists(FICHEIRO) && !Files.isDirectory(FICHEIRO)) {
            Files.move(FICHEIRO, COPIA, StandardCopyOption.REPLACE_EXISTING);
        }
        outOriginal = System.out;
        errOriginal = System.err;
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() throws IOException {
        System.setOut(outOriginal);
        System.setErr(errOriginal);
        apagar(FICHEIRO);
        if (Files.exists(COPIA)) {
            Files.move(COPIA, FICHEIRO, StandardCopyOption.REPLACE_EXISTING);
        }
        out = null;
        err = null;
    }

    private static void apagar(Path caminho) throws IOException {
        if (Files.isDirectory(caminho)) {
            try (var conteudo = Files.list(caminho)) {
                for (Path p : conteudo.toList()) {
                    Files.delete(p);
                }
            }
        }
        Files.deleteIfExists(caminho);
    }

    private String saida() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String erros() {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("O construtor implícito cria uma instância (a classe só tem métodos estáticos)")
    void scoreboardManager() {
        assertNotNull(new ScoreboardManager(), "Error: expected a ScoreboardManager instance but got null");
    }

    @Test
    @DisplayName("carregarScores devolve uma lista vazia quando o ficheiro não existe")
    void carregarScores1() {
        List<ScoreEntry> scores = ScoreboardManager.carregarScores();
        assertAll("Ficheiro inexistente",
                () -> assertNotNull(scores, "Error: expected an empty list but got null"),
                () -> assertTrue(scores.isEmpty(), "Error: expected 0 entries but got " + scores.size()));
    }

    @Test
    @DisplayName("carregarScores lê as partidas guardadas no ficheiro JSON")
    void carregarScores2() throws IOException {
        Files.writeString(FICHEIRO, """
                [
                  {"data": "2026-11-20 10:00:00", "totalTiros": 30, "naviosAfundados": 11, "vitoria": true},
                  {"data": "2026-11-20 11:00:00", "totalTiros": 45, "naviosAfundados": 4, "vitoria": false}
                ]""", StandardCharsets.UTF_8);
        List<ScoreEntry> scores = ScoreboardManager.carregarScores();
        assertAll("Partidas lidas do ficheiro",
                () -> assertEquals(2, scores.size(), "Error: expected 2 entries but got " + scores.size()),
                () -> assertEquals("2026-11-20 10:00:00", scores.get(0).getData(), "Error: expected first date 2026-11-20 10:00:00 but got " + scores.get(0).getData()),
                () -> assertEquals(30, scores.get(0).getTotalTiros(), "Error: expected 30 shots but got " + scores.get(0).getTotalTiros()),
                () -> assertTrue(scores.get(0).isVitoria(), "Error: expected the first game to be a victory"),
                () -> assertEquals(4, scores.get(1).getNaviosAfundados(), "Error: expected 4 sunk ships but got " + scores.get(1).getNaviosAfundados()),
                () -> assertFalse(scores.get(1).isVitoria(), "Error: expected the second game to be a defeat"));
    }

    @Test
    @DisplayName("carregarScores devolve uma lista vazia quando o ficheiro existe mas está vazio (Gson devolve null)")
    void carregarScores3() throws IOException {
        Files.writeString(FICHEIRO, "", StandardCharsets.UTF_8);
        List<ScoreEntry> scores = ScoreboardManager.carregarScores();
        assertAll("Ficheiro vazio",
                () -> assertNotNull(scores, "Error: expected an empty list but got null"),
                () -> assertTrue(scores.isEmpty(), "Error: expected 0 entries but got " + scores.size()));
    }

    @Test
    @DisplayName("carregarScores devolve uma lista vazia e escreve o erro quando o ficheiro não pode ser lido")
    void carregarScores4() throws IOException {
        Files.createDirectory(FICHEIRO);
        List<ScoreEntry> scores = ScoreboardManager.carregarScores();
        assertAll("Erro de leitura",
                () -> assertTrue(scores.isEmpty(), "Error: expected 0 entries but got " + scores.size()),
                () -> assertTrue(erros().contains("Erro ao carregar scoreboard"),
                        "Error: expected a read error message but got '" + erros() + "'"));
    }

    @Test
    @DisplayName("registarPartida acrescenta a partida ao ficheiro e confirma a gravação")
    void registarPartida1() {
        ScoreboardManager.registarPartida(30, 11, true);
        ScoreboardManager.registarPartida(50, 3, false);
        List<ScoreEntry> scores = ScoreboardManager.carregarScores();
        assertAll("Partidas gravadas",
                () -> assertTrue(Files.exists(FICHEIRO), "Error: expected scoreboard.json to exist but it does not"),
                () -> assertEquals(2, scores.size(), "Error: expected 2 entries but got " + scores.size()),
                () -> assertEquals(30, scores.get(0).getTotalTiros(), "Error: expected 30 shots but got " + scores.get(0).getTotalTiros()),
                () -> assertTrue(scores.get(0).isVitoria(), "Error: expected the first game to be a victory"),
                () -> assertEquals(3, scores.get(1).getNaviosAfundados(), "Error: expected 3 sunk ships but got " + scores.get(1).getNaviosAfundados()),
                () -> assertFalse(scores.get(1).isVitoria(), "Error: expected the second game to be a defeat"),
                () -> assertTrue(saida().contains("Partida guardada com sucesso"),
                        "Error: expected a success message but got '" + saida() + "'"));
    }

    @Test
    @DisplayName("registarPartida escreve o erro quando o ficheiro não pode ser gravado")
    void registarPartida2() throws IOException {
        Files.createDirectory(FICHEIRO);
        ScoreboardManager.registarPartida(30, 11, true);
        assertAll("Erro de gravação",
                () -> assertTrue(erros().contains("Erro ao gravar no scoreboard"),
                        "Error: expected a write error message but got '" + erros() + "'"),
                () -> assertFalse(saida().contains("Partida guardada com sucesso"),
                        "Error: expected no success message but got '" + saida() + "'"));
    }

    @Test
    @DisplayName("imprimirScoreboard indica que não há jogos quando o histórico está vazio")
    void imprimirScoreboard1() {
        ScoreboardManager.imprimirScoreboard();
        assertAll("Histórico vazio",
                () -> assertTrue(saida().contains("SCOREBOARD DE JOGOS PASSADOS"), "Error: expected the scoreboard title but got '" + saida() + "'"),
                () -> assertTrue(saida().contains("Nenhum jogo registado até ao momento."), "Error: expected the empty message but got '" + saida() + "'"));
    }

    @Test
    @DisplayName("imprimirScoreboard lista uma partida numerada quando há um jogo")
    void imprimirScoreboard2() {
        ScoreboardManager.registarPartida(30, 11, true);
        out.reset();
        ScoreboardManager.imprimirScoreboard();
        assertAll("Um jogo registado",
                () -> assertTrue(saida().contains("1. ["), "Error: expected entry '1.' but got '" + saida() + "'"),
                () -> assertTrue(saida().contains("VITÓRIA"), "Error: expected 'VITÓRIA' but got '" + saida() + "'"),
                () -> assertFalse(saida().contains("Nenhum jogo registado"), "Error: expected no empty message but got '" + saida() + "'"),
                () -> assertFalse(saida().contains("2. ["), "Error: expected only one entry but got '" + saida() + "'"));
    }

    @Test
    @DisplayName("imprimirScoreboard lista várias partidas pela ordem em que foram registadas")
    void imprimirScoreboard3() {
        ScoreboardManager.registarPartida(30, 11, true);
        ScoreboardManager.registarPartida(50, 3, false);
        out.reset();
        ScoreboardManager.imprimirScoreboard();
        String texto = saida();
        assertAll("Vários jogos registados",
                () -> assertTrue(texto.contains("1. ["), "Error: expected entry '1.' but got '" + texto + "'"),
                () -> assertTrue(texto.contains("2. ["), "Error: expected entry '2.' but got '" + texto + "'"),
                () -> assertTrue(texto.indexOf("VITÓRIA") < texto.indexOf("DERROTA"),
                        "Error: expected the victory before the defeat but got '" + texto + "'"));
    }
}
