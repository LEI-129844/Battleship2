package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for class ScoreEntry.
 * Author: Tomás Leal (LEI-111491)
 * Date: 2026-11-20 18:00
 * Cyclomatic Complexity:
 * - ScoreEntry(): 1
 * - ScoreEntry(int, int, boolean): 1
 * - getData(): 1
 * - getTotalTiros(): 1
 * - getNaviosAfundados(): 1
 * - isVitoria(): 1
 * - toString(): 2
 */
class ScoreEntryTest {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ScoreEntry entry;

    @BeforeEach
    void setUp() {
        entry = new ScoreEntry(42, 5, true);
    }

    @AfterEach
    void tearDown() {
        entry = null;
    }

    @Test
    @DisplayName("Construtor sem argumentos (usado pelo Gson) deixa os campos com os valores por omissão")
    void scoreEntryDefault() {
        ScoreEntry vazio = new ScoreEntry();
        assertAll("Campos por omissão",
                () -> assertNull(vazio.getData(), "Error: expected null date but got " + vazio.getData()),
                () -> assertEquals(0, vazio.getTotalTiros(), "Error: expected 0 shots but got " + vazio.getTotalTiros()),
                () -> assertEquals(0, vazio.getNaviosAfundados(), "Error: expected 0 sunk ships but got " + vazio.getNaviosAfundados()),
                () -> assertFalse(vazio.isVitoria(), "Error: expected no victory by default but got a victory"));
    }

    @Test
    @DisplayName("Construtor com argumentos guarda os valores e a data atual no formato yyyy-MM-dd HH:mm:ss")
    void scoreEntry() {
        LocalDateTime antes = LocalDateTime.now().withNano(0);
        ScoreEntry nova = new ScoreEntry(10, 2, false);
        LocalDateTime depois = LocalDateTime.now();
        LocalDateTime data = LocalDateTime.parse(nova.getData(), FORMATO_DATA);
        assertAll("Valores guardados pelo construtor",
                () -> assertEquals(10, nova.getTotalTiros(), "Error: expected 10 shots but got " + nova.getTotalTiros()),
                () -> assertEquals(2, nova.getNaviosAfundados(), "Error: expected 2 sunk ships but got " + nova.getNaviosAfundados()),
                () -> assertFalse(nova.isVitoria(), "Error: expected a defeat but got a victory"),
                () -> assertFalse(data.isBefore(antes), "Error: expected date not before " + antes + " but got " + data),
                () -> assertFalse(data.isAfter(depois), "Error: expected date not after " + depois + " but got " + data));
    }

    @Test
    @DisplayName("getData devolve a data de registo da partida")
    void getData() {
        assertNotNull(entry.getData(), "Error: expected a non-null date but got null");
        assertTrue(entry.getData().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "Error: expected format yyyy-MM-dd HH:mm:ss but got " + entry.getData());
    }

    @Test
    @DisplayName("getTotalTiros devolve o número de tiros disparados")
    void getTotalTiros() {
        assertEquals(42, entry.getTotalTiros(), "Error: expected 42 shots but got " + entry.getTotalTiros());
    }

    @Test
    @DisplayName("getNaviosAfundados devolve o número de navios afundados")
    void getNaviosAfundados() {
        assertEquals(5, entry.getNaviosAfundados(), "Error: expected 5 sunk ships but got " + entry.getNaviosAfundados());
    }

    @Test
    @DisplayName("isVitoria devolve o resultado da partida")
    void isVitoria() {
        assertTrue(entry.isVitoria(), "Error: expected a victory but got a defeat");
    }

    @Test
    @DisplayName("toString de uma vitória mostra 'VITÓRIA', os tiros e os navios afundados")
    void toString1() {
        String esperado = "[" + entry.getData() + "] Resultado: VITÓRIA | Tiros Disparados: 42 | Navios Afundados: 5";
        assertEquals(esperado, entry.toString(), "Error: expected '" + esperado + "' but got '" + entry + "'");
    }

    @Test
    @DisplayName("toString de uma derrota mostra 'DERROTA'")
    void toString2() {
        ScoreEntry derrota = new ScoreEntry(30, 1, false);
        String esperado = "[" + derrota.getData() + "] Resultado: DERROTA | Tiros Disparados: 30 | Navios Afundados: 1";
        assertEquals(esperado, derrota.toString(), "Error: expected '" + esperado + "' but got '" + derrota + "'");
    }
}
