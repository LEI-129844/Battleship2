package restserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameSessionTest {

    @Test
    @DisplayName("Inicializa uma sessao de jogo com os dados fornecidos")
    void initializesGameSession() {
        GameSession session = new GameSession("game-1", "Alice", "http://localhost:9090");

        assertAll(
                () -> assertEquals("game-1", session.getGameId()),
                () -> assertEquals("Alice", session.getPlayerName()),
                () -> assertEquals("http://localhost:9090", session.getCallbackUrl()),
                () -> assertNotNull(session.getGame()),
                () -> assertEquals(3, session.getShotsPerTurn()),
                () -> assertFalse(session.isGameOver()),
                () -> assertNull(session.getWinner())
        );
    }

    @Test
    @DisplayName("Marca a inteligencia artificial como vencedora")
    void marksAiWinner() {
        GameSession session = new GameSession("game-1", "Alice", "http://localhost:9090");

        session.markAiWins();

        assertAll(
                () -> assertTrue(session.isGameOver()),
                () -> assertEquals("AI_WINS", session.getWinner())
        );
    }

    @Test
    @DisplayName("Marca o estudante como vencedor")
    void marksStudentWinner() {
        GameSession session = new GameSession("game-1", "Alice", "http://localhost:9090");

        session.markStudentWins();

        assertAll(
                () -> assertTrue(session.isGameOver()),
                () -> assertEquals("STUDENT_WINS", session.getWinner())
        );
    }
}
