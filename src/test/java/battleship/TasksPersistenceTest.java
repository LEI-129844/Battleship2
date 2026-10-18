package battleship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TasksPersistenceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void persistsTheLatestMoveCreatedByTheGame() {
        Path databaseFile = tempDirectory.resolve("tasks-history.db");
        MoveDatabase database = new MoveDatabase(databaseFile);
        long gameId = database.createGame();
        Game game = new Game(new Fleet());

        game.fireShots(List.of(
                new Position(0, 0),
                new Position(0, 1),
                new Position(0, 2)));
        Tasks.persistLatestMove(database, gameId, game);

        List<MoveDatabase.StoredShot> storedShots =
                new MoveDatabase(databaseFile).findShotsForGame(gameId);
        assertEquals(3, storedShots.size());
        assertEquals("A1", storedShots.get(0).coordinate());
        assertEquals("AGUA", storedShots.get(0).result());
    }
}
