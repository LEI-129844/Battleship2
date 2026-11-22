package restserver;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 129844 - Henrique Fernandes
 * (Issue "Unit testing task (129844)").
 *
 * Cada teste cria um registo novo no @BeforeEach, por isso não há estado partilhado entre testes.
 * Os ids das sessões são UUIDs aleatórios, por isso os testes obtêm o id a partir da própria
 * sessão criada em vez de assumir valores fixos..
 */
class GameRegistryTest {

    private GameRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new GameRegistry();
    }

    @AfterEach
    void tearDown() {
        registry = null;
    }

    @Test
    @DisplayName("createSession devolve uma sessão não nula")
    void createSessionReturnsNonNull() {
        GameSession session = registry.createSession("Ana", "http://localhost:9000/cb");
        assertNotNull(session, "createSession não deve devolver null");
    }

    @Test
    @DisplayName("createSession guarda a sessão: getSession devolve a mesma instância")
    void createSessionStoresSession() {
        GameSession session = registry.createSession("Ana", "http://localhost:9000/cb");
        assertAll(
                () -> assertNotNull(session),
                () -> assertSame(session, findById(session),
                        "A sessão criada devia ficar guardada no registo")
        );
    }

    @Test
    @DisplayName("Duas sessões criadas são instâncias distintas")
    void createSessionTwiceReturnsDistinctSessions() {
        GameSession s1 = registry.createSession("Ana", "http://a");
        GameSession s2 = registry.createSession("Rui", "http://b");
        assertAll(
                () -> assertNotSame(s1, s2, "Devem ser instâncias diferentes"),
                () -> assertSame(s1, findById(s1)),
                () -> assertSame(s2, findById(s2))
        );
    }

    @Test
    @DisplayName("createSession aceita nome e callback nulos (o registo não valida)")
    void createSessionWithNullArguments() {
        // Comportamento atual: o registo não valida. Se GameSession lançar
        // exceção com nulls, trocar por assertThrows.
        assertDoesNotThrow(() -> registry.createSession(null, null));
    }

    @Test
    @DisplayName("getSession com id inexistente devolve null")
    void getSessionUnknownIdReturnsNull() {
        assertNull(registry.getSession("nao-existe"),
                "Id desconhecido devia devolver null");
    }

    @Test
    @DisplayName("getSession com id nulo lança NullPointerException (ConcurrentHashMap)")
    void getSessionNullIdThrows() {
        assertThrows(NullPointerException.class, () -> registry.getSession(null));
    }

    @Test
    @DisplayName("removeSession remove uma sessão existente")
    void removeSessionRemovesExisting() {
        GameSession session = registry.createSession("Ana", "http://a");
        String id = idOf(session);
        registry.removeSession(id);
        assertNull(registry.getSession(id), "A sessão devia ter sido removida");
    }

    @Test
    @DisplayName("removeSession com id inexistente não lança exceção")
    void removeSessionUnknownIdDoesNothing() {
        assertDoesNotThrow(() -> registry.removeSession("nao-existe"));
    }

    @Test
    @DisplayName("removeSession só remove a sessão indicada")
    void removeSessionKeepsOthers() {
        GameSession s1 = registry.createSession("Ana", "http://a");
        GameSession s2 = registry.createSession("Rui", "http://b");
        registry.removeSession(idOf(s1));
        assertAll(
                () -> assertNull(registry.getSession(idOf(s1))),
                () -> assertSame(s2, registry.getSession(idOf(s2)))
        );
    }

    // ---- auxiliares ------------------------------------------------------

    /** Obtém o id da sessão. AJUSTAR ao getter real do GameSession. */
    private static String idOf(GameSession session) {
        return session.getGameId();
    }

    private GameSession findById(GameSession session) {
        return registry.getSession(idOf(session));
    }
}