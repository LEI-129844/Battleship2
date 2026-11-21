package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for interface IMove (método estático readMove).
 * Author: Tomás Leal (LEI-111491)
 * Date: 2026-11-20 21:20
 * Cyclomatic Complexity:
 * - readMove(int, Scanner): 2
 *
 * Os restantes métodos de IMove são abstratos e estão testados na implementação (MoveTest).
 */
class IMoveTest {

    private Scanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new Scanner("2 1 4 6 7");
    }

    @AfterEach
    void tearDown() {
        scanner.close();
        scanner = null;
    }

    @Test
    @DisplayName("readMove lê o número de tiros e as coordenadas de cada tiro (ciclo executado)")
    void readMove1() {
        Move lida = IMove.readMove(3, scanner);
        assertAll("Jogada lida",
                () -> assertEquals(3, lida.getNumber(), "Error: expected move number 3 but got " + lida.getNumber()),
                () -> assertEquals(List.of(new Position(1, 4), new Position(6, 7)), lida.getShots(),
                        "Error: expected shots [(1,4), (6,7)] but got " + lida.getShots()),
                () -> assertTrue(lida.getShotResults().isEmpty(),
                        "Error: expected no results yet but got " + lida.getShotResults().size()));
    }

    @Test
    @DisplayName("readMove com zero tiros devolve uma jogada sem tiros (ciclo não executado)")
    void readMove2() {
        Move lida = IMove.readMove(1, new Scanner("0"));
        assertAll("Jogada vazia",
                () -> assertEquals(1, lida.getNumber(), "Error: expected move number 1 but got " + lida.getNumber()),
                () -> assertTrue(lida.getShots().isEmpty(), "Error: expected no shots but got " + lida.getShots().size()));
    }

    @Test
    @DisplayName("readMove lança NoSuchElementException quando faltam coordenadas na entrada")
    void readMove3() {
        assertThrows(NoSuchElementException.class, () -> IMove.readMove(2, new Scanner("2 1 4")),
                "Error: expected NoSuchElementException for incomplete input but none was thrown");
    }
}
