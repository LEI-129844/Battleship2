/**
 * 129844 - Henrique Fernandes
 * (Issue "Unit testing task (129844)").
 *
 * A classe só tem o main, que imprime o banner e delega em Tasks.menu(), e o construtor por omissão.
 * O main é chamado com a implementação real, com o System.in substituído por uma entrada vazia e o
 * System.out capturado, para verificar o que é escrito. Como o menu lê do teclado, uma entrada vazia
 * faz com que ele termine (normalmente ou com uma exceção de leitura); o teste só exige que o banner
 * seja escrito antes disso. O timeout evita que o teste bloqueie se o menu ficar em ciclo.
 *
 */
package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class MainTest {

    private static final String BANNER = "***  Battleship  ***";

    private InputStream originalIn;
    private PrintStream originalOut;
    private ByteArrayOutputStream capturedOut;

    @BeforeEach
    void setUp() {
        originalIn = System.in;
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setIn(new ByteArrayInputStream(new byte[0]));
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        System.setIn(originalIn);
        System.setOut(originalOut);
    }

    /** Corre o main; ignora exceções vindas do menu (entrada vazia), mas não o timeout. */
    private void runMain() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            try {
                Main.main(new String[0]);
            } catch (RuntimeException expected) {
                // O menu pode falhar a ler da entrada vazia (ex.: NoSuchElementException).
            }
        }, "O main não devia ficar bloqueado com a entrada vazia");
    }

    @Test
    @DisplayName("main escreve o banner do jogo")
    void mainPrintsBanner() {
        runMain();
        String output = capturedOut.toString(StandardCharsets.UTF_8);
        assertTrue(output.contains(BANNER),
                "O banner devia ser escrito. Saída obtida: " + output);
    }

    @Test
    @DisplayName("O banner é a primeira coisa escrita pelo main")
    void bannerIsFirstOutput() {
        runMain();
        String output = capturedOut.toString(StandardCharsets.UTF_8);
        assertTrue(output.startsWith(BANNER),
                "O banner devia vir antes do menu. Saída obtida: " + output);
    }

    @Test
    @DisplayName("main aceita um array de argumentos com conteúdo (os argumentos são ignorados)")
    void mainIgnoresArguments() {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            try {
                Main.main(new String[]{"qualquer", "coisa"});
            } catch (RuntimeException expected) {
                // Ver runMain()
            }
        });
        assertTrue(capturedOut.toString(StandardCharsets.UTF_8).contains(BANNER));
    }

    @Test
    @DisplayName("O construtor por omissão cria uma instância")
    void defaultConstructorCreatesInstance() {
        assertNotNull(new Main(), "A instância não devia ser null");
    }
}