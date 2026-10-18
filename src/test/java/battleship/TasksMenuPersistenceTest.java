package battleship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TasksMenuPersistenceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void consoleRajadaIsStoredInTheDatabase() {
        Path databaseFile = tempDirectory.resolve("console-history.db");
        MoveDatabase database = new MoveDatabase(databaseFile);
        InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        String commands = "gerafrota\nrajada A1 B2 C3\ndesisto\n";

        try (PrintStream testOutput = new PrintStream(capturedOutput, true, StandardCharsets.UTF_8)) {
            System.setIn(new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)));
            System.setOut(testOutput);
            Tasks.menu(database);
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }

        Matcher gameIdMatch = Pattern.compile(
                "Partida registada na base de dados\\. ID: (\\d+)")
                .matcher(capturedOutput.toString(StandardCharsets.UTF_8));
        assertTrue(gameIdMatch.find(), "A consola deve mostrar o identificador da partida");
        long gameId = Long.parseLong(gameIdMatch.group(1));

        List<MoveDatabase.StoredShot> storedShots =
                new MoveDatabase(databaseFile).findShotsForGame(gameId);
        assertEquals(3, storedShots.size());
        assertEquals(List.of("A1", "B2", "C3"),
                storedShots.stream().map(MoveDatabase.StoredShot::coordinate).toList());
    }
}
