package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for class Move.
 * Author: Tomás Leal (LEI-111491)
 * Date: 2026-11-20 21:00
 * Cyclomatic Complexity:
 * - Move(int, List, List): 1
 * - toString(): 1
 * - getNumber(): 1
 * - getShots(): 1
 * - getShotResults(): 1
 * - processEnemyFire(boolean): 37
 *
 * Os testes de processEnemyFire seguem os caminhos independentes do método: tipo de cada tiro
 * (inválido, repetido, na água, num navio, afundou o navio), modo verbose ligado/desligado e
 * todas as variantes da mensagem (singular/plural, separadores e tiros fora do tabuleiro).
 * Game.NUMBER_SHOTS = 3, por isso os tiros inválidos contam como tiros "exteriores".
 *
 * Ramos/linhas não cobríveis sem alterar o código:
 * - "if (validShots > 0)" dentro de "if (repeatedShots > 0)": com tiros repetidos e 0 válidos o
 *   método segue sempre o primeiro ramo ("validShots == 0 && repeatedShots > 0");
 * - o catch de JsonProcessingException: o Jackson não falha a serializar mapas de números e textos.
 */
class MoveTest {

    private Move move;
    private PrintStream outOriginal;
    private ByteArrayOutputStream out;

    @BeforeEach
    void setUp() {
        move = new Move(7, new ArrayList<>(List.of(new Position(1, 1), new Position(2, 2))), new ArrayList<>());
        outOriginal = System.out;
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setOut(outOriginal);
        move = null;
        out = null;
    }

    // ---------------------------------------------------------------- auxiliares

    private static IGame.ShotResult invalido() {
        return new IGame.ShotResult(false, false, null, false);
    }

    private static IGame.ShotResult repetido() {
        return new IGame.ShotResult(true, true, null, false);
    }

    private static IGame.ShotResult naAgua() {
        return new IGame.ShotResult(true, false, null, false);
    }

    private static IGame.ShotResult acerto(IShip navio) {
        return new IGame.ShotResult(true, false, navio, false);
    }

    private static IGame.ShotResult afundou(IShip navio) {
        return new IGame.ShotResult(true, false, navio, true);
    }

    private static IShip barca() {
        return new Barge(Compass.NORTH, new Position(0, 0));
    }

    private static IShip caravela() {
        return new Caravel(Compass.NORTH, new Position(5, 5));
    }

    /** Processa os resultados em modo verbose e devolve a linha "Jogada nº... -> ..." escrita na consola. */
    private String mensagem(IGame.ShotResult... resultados) {
        Move m = new Move(1, new ArrayList<>(), List.of(resultados));
        m.processEnemyFire(true);
        String saida = out.toString(StandardCharsets.UTF_8);
        int inicio = saida.indexOf("-> ");
        assertTrue(inicio >= 0, "Error: expected a line 'Jogada nº1 -> ...' but got '" + saida + "'");
        return saida.substring(inicio + 3, saida.indexOf(System.lineSeparator(), inicio));
    }

    // ---------------------------------------------------------------- construtor, toString e getters

    @Test
    @DisplayName("Construtor guarda o número, os tiros e os resultados da jogada")
    void move() {
        List<IPosition> tiros = List.of(new Position(3, 3));
        List<IGame.ShotResult> resultados = List.of(naAgua());
        Move m = new Move(4, tiros, resultados);
        assertAll("Estado inicial",
                () -> assertEquals(4, m.getNumber(), "Error: expected move number 4 but got " + m.getNumber()),
                () -> assertSame(tiros, m.getShots(), "Error: expected the same list of shots given to the constructor"),
                () -> assertSame(resultados, m.getShotResults(), "Error: expected the same list of results given to the constructor"));
    }

    @Test
    @DisplayName("toString mostra o número da jogada, o número de tiros e o número de resultados")
    void testToString() {
        assertEquals("Move{number=7, shots=2, results=0}", move.toString(),
                "Error: expected 'Move{number=7, shots=2, results=0}' but got '" + move + "'");
    }

    @Test
    @DisplayName("getNumber devolve o número da jogada")
    void getNumber() {
        assertEquals(7, move.getNumber(), "Error: expected 7 but got " + move.getNumber());
    }

    @Test
    @DisplayName("getShots devolve a lista de tiros da jogada")
    void getShots() {
        assertAll("Tiros",
                () -> assertEquals(2, move.getShots().size(), "Error: expected 2 shots but got " + move.getShots().size()),
                () -> assertEquals(new Position(1, 1), move.getShots().get(0), "Error: expected first shot (1,1) but got " + move.getShots().get(0)));
    }

    @Test
    @DisplayName("getShotResults devolve a lista de resultados da jogada")
    void getShotResults() {
        assertTrue(move.getShotResults().isEmpty(), "Error: expected no results but got " + move.getShotResults().size());
    }

    // ---------------------------------------------------------------- processEnemyFire: JSON (verbose desligado)

    @Test
    @DisplayName("processEnemyFire sem verbose devolve o JSON com todas as contagens e não escreve a mensagem")
    void processEnemyFire1() {
        IShip barca = barca();
        IShip caravela = caravela();
        Move m = new Move(2, new ArrayList<>(), List.of(afundou(barca), acerto(caravela), invalido()));
        String json = m.processEnemyFire(false);
        String saida = out.toString(StandardCharsets.UTF_8);
        assertAll("JSON da jogada",
                () -> assertTrue(json.contains("\"validShots\" : 2"), "Error: expected validShots 2 but got " + json),
                () -> assertTrue(json.contains("\"outsideShots\" : 1"), "Error: expected outsideShots 1 but got " + json),
                () -> assertTrue(json.contains("\"repeatedShots\" : 0"), "Error: expected repeatedShots 0 but got " + json),
                () -> assertTrue(json.contains("\"missedShots\" : 0"), "Error: expected missedShots 0 but got " + json),
                () -> assertTrue(json.contains("\"type\" : \"Barca\""), "Error: expected the sunk barge in sunkBoats but got " + json),
                () -> assertTrue(json.contains("\"hits\" : 1"), "Error: expected 1 hit on the caravel but got " + json),
                () -> assertFalse(saida.contains("Jogada nº"), "Error: expected no verbose line but got '" + saida + "'"));
    }

    @Test
    @DisplayName("processEnemyFire não lista em hitsOnBoats um tipo de navio que já foi afundado")
    void processEnemyFire2() {
        IShip barca1 = barca();
        IShip barca2 = new Barge(Compass.NORTH, new Position(9, 9));
        Move m = new Move(3, new ArrayList<>(), List.of(afundou(barca1), acerto(barca2), naAgua()));
        String json = m.processEnemyFire(false);
        String hitsOnBoats = json.substring(json.indexOf("\"hitsOnBoats\""));
        assertAll("Acertos em tipos afundados",
                () -> assertTrue(json.contains("\"sunkBoats\""), "Error: expected a sunkBoats entry but got " + json),
                () -> assertFalse(hitsOnBoats.contains("Barca"), "Error: expected no 'Barca' in hitsOnBoats but got " + hitsOnBoats));
    }

    // ---------------------------------------------------------------- processEnemyFire: mensagens em modo verbose

    @Test
    @DisplayName("Só tiros repetidos (plural) e um tiro exterior")
    void processEnemyFire3() {
        assertEquals("2 tiros repetidos, 1 tiro exterior", mensagem(repetido(), repetido(), invalido()),
                "Error: unexpected message for 2 repeated shots and 1 outside shot");
    }

    @Test
    @DisplayName("Só um tiro repetido (singular) e dois tiros exteriores")
    void processEnemyFire4() {
        assertEquals("1 tiro repetido, 2 tiros exteriores", mensagem(repetido(), invalido(), invalido()),
                "Error: unexpected message for 1 repeated shot and 2 outside shots");
    }

    @Test
    @DisplayName("Todos os tiros fora do tabuleiro: só aparece a contagem de tiros exteriores")
    void processEnemyFire5() {
        assertEquals("3 tiros exteriores", mensagem(invalido(), invalido(), invalido()),
                "Error: unexpected message for 3 outside shots");
    }

    @Test
    @DisplayName("Um tiro válido na água (singular) e dois tiros exteriores")
    void processEnemyFire6() {
        assertEquals("1 tiro válido: 1 tiro na água, 2 tiros exteriores", mensagem(naAgua(), invalido(), invalido()),
                "Error: unexpected message for 1 shot in the water and 2 outside shots");
    }

    @Test
    @DisplayName("Três tiros na água (plural)")
    void processEnemyFire7() {
        assertEquals("3 tiros válidos: 3 tiros na água", mensagem(naAgua(), naAgua(), naAgua()),
                "Error: unexpected message for 3 shots in the water");
    }

    @Test
    @DisplayName("Dois navios do mesmo tipo afundados (plural) e um acerto noutro navio, sem tiros na água")
    void processEnemyFire8() {
        IShip barca1 = barca();
        IShip barca2 = new Barge(Compass.NORTH, new Position(9, 9));
        // Comportamento atual: setLength(length - 2) remove o "+" final mas deixa um espaço no fim da mensagem.
        assertEquals("3 tiros válidos: 2 Barcas ao fundo + 1 tiro num(a) Caravela ",
                mensagem(afundou(barca1), afundou(barca2), acerto(caravela())),
                "Error: unexpected message for 2 sunk barges and 1 hit on a caravel");
    }

    @Test
    @DisplayName("Dois acertos no mesmo navio (plural) e um tiro na água")
    void processEnemyFire9() {
        IShip caravela = caravela();
        assertEquals("3 tiros válidos: 2 tiros num(a) Caravela + 1 tiro na água",
                mensagem(acerto(caravela), acerto(caravela), naAgua()),
                "Error: unexpected message for 2 hits on a caravel and 1 shot in the water");
    }

    @Test
    @DisplayName("Tiro válido na água seguido de dois tiros repetidos (separador e plural)")
    void processEnemyFire10() {
        assertEquals("1 tiro válido: 1 tiro na água, 2 tiros repetidos", mensagem(naAgua(), repetido(), repetido()),
                "Error: unexpected message for 1 shot in the water and 2 repeated shots");
    }

    @Test
    @DisplayName("Um navio afundado, um tiro repetido e um tiro exterior")
    void processEnemyFire11() {
        String msg = mensagem(afundou(barca()), repetido(), invalido());
        assertAll("Afundado, repetido e exterior",
                () -> assertTrue(msg.startsWith("1 tiro válido: 1 Barca ao fundo"), "Error: expected the sunk barge first but got '" + msg + "'"),
                () -> assertTrue(msg.contains(", 1 tiro repetido"), "Error: expected '1 tiro repetido' but got '" + msg + "'"),
                () -> assertTrue(msg.endsWith(", 1 tiro exterior"), "Error: expected '1 tiro exterior' at the end but got '" + msg + "'"));
    }

    @Test
    @DisplayName("Um navio afundado, um acerto noutro tipo de navio e um tiro na água, sem tiros exteriores")
    void processEnemyFire12() {
        assertEquals("3 tiros válidos: 1 Barca ao fundo + 1 tiro num(a) Caravela + 1 tiro na água",
                mensagem(afundou(barca()), acerto(caravela()), naAgua()),
                "Error: unexpected message for 1 sunk barge, 1 hit on a caravel and 1 shot in the water");
    }

    @Test
    @DisplayName("Só acertos num navio que não afundou, sem tiros na água, e dois tiros exteriores")
    void processEnemyFire13() {
        // Comportamento atual: o "+" final é removido mas fica um espaço antes da vírgula.
        assertEquals("1 tiro válido: 1 tiro num(a) Caravela , 2 tiros exteriores",
                mensagem(acerto(caravela()), invalido(), invalido()),
                "Error: unexpected message for 1 hit on a caravel and 2 outside shots");
    }

    @Test
    @DisplayName("processEnemyFire escreve o número da jogada e o JSON na consola em modo verbose")
    void processEnemyFire14() {
        Move m = new Move(5, new ArrayList<>(), List.of(naAgua(), naAgua(), naAgua()));
        String json = m.processEnemyFire(true);
        String saida = out.toString(StandardCharsets.UTF_8);
        assertAll("Saída na consola",
                () -> assertTrue(saida.contains("Jogada nº5 -> "), "Error: expected 'Jogada nº5 -> ' but got '" + saida + "'"),
                () -> assertTrue(saida.contains(json), "Error: expected the JSON to be printed but got '" + saida + "'"));
    }
}
