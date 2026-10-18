package battleship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveDatabaseTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesShotCoordinatesAndResultsAcrossDatabaseReopen() {
        Path databaseFile = tempDirectory.resolve("game-history.db");
        MoveDatabase database = new MoveDatabase(databaseFile);
        long gameId = database.createGame();
        Barge ship = new Barge(Compass.NORTH, new Position(0, 0));

        database.saveMove(gameId, new Move(
                1,
                List.of(new Position(0, 0), new Position(0, 1), new Position(0, 2)),
                List.of(
                        new IGame.ShotResult(true, false, null, false),
                        new IGame.ShotResult(true, true, null, false),
                        new IGame.ShotResult(true, false, ship, true))));
        database.saveMove(gameId, new Move(
                2,
                List.of(new Position(10, 10), new Position(1, 1), new Position(1, 2)),
                List.of(
                        new IGame.ShotResult(false, false, null, false),
                        new IGame.ShotResult(true, false, ship, false),
                        new IGame.ShotResult(true, false, null, false))));

        MoveDatabase reopenedDatabase = new MoveDatabase(databaseFile);
        List<MoveDatabase.StoredShot> savedShots = reopenedDatabase.findShotsForGame(gameId);

        assertTrue(Files.exists(databaseFile));
        assertEquals(6, savedShots.size());
        assertEquals(1, savedShots.get(0).moveNumber());
        assertEquals(1, savedShots.get(0).shotNumber());
        assertEquals("A1", savedShots.get(0).coordinate());
        assertEquals("AGUA", savedShots.get(0).result());
        assertEquals("REPETIDO", savedShots.get(1).result());
        assertEquals("AFUNDADO", savedShots.get(2).result());
        assertEquals(2, savedShots.get(3).moveNumber());
        assertEquals("FORA_DO_TABULEIRO", savedShots.get(3).result());
        assertEquals("ATINGIDO", savedShots.get(4).result());
        assertEquals("AGUA", savedShots.get(5).result());
    }
}
