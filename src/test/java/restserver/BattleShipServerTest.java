/**
 * 129844 - Henrique Fernandes
 * (Issue "Unit testing task (129844)").
 *
 * A classe só tem o main, que delega em SpringApplication.run, e o construtor por omissão.
 * O main é chamado com a implementação real, mas com argumentos que impedem o arranque do
 * servidor web (web-application-type=none, e porta 0 por segurança), para o teste não
 * bloquear nem colidir com a porta 8080. Isto verifica ainda que o contexto Spring
 * (component scan dos beans do pacote restserver) consegue ser criado..
 */
package restserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.jupiter.api.Assertions.*;

class BattleshipServerTest {

    @Test
    @DisplayName("main arranca o contexto Spring sem servidor web e sem lançar exceções")
    void mainStartsContextWithoutWebServer() {
        assertDoesNotThrow(() -> BattleshipServer.main(new String[]{
                "--spring.main.web-application-type=none",
                "--spring.main.banner-mode=off",
                "--server.port=0"
        }), "O arranque do contexto Spring não devia falhar");
    }

    @Test
    @DisplayName("O construtor por omissão cria uma instância")
    void defaultConstructorCreatesInstance() {
        assertNotNull(new BattleshipServer(), "A instância não devia ser null");
    }

    @Test
    @DisplayName("A classe está anotada com @SpringBootApplication")
    void classIsAnnotatedWithSpringBootApplication() {
        assertTrue(BattleshipServer.class.isAnnotationPresent(SpringBootApplication.class),
                "Falta a anotação @SpringBootApplication");
    }
}