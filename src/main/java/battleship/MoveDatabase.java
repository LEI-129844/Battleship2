package battleship;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Guarda o histórico das partidas e dos tiros numa base de dados SQLite local. */
public final class MoveDatabase {

    private static final String DEFAULT_DATABASE_FILE = "battleship.db";

    private final String jdbcUrl;

    /** Cria ou abre a base de dados local do jogo. */
    public MoveDatabase() {
        this(Path.of(DEFAULT_DATABASE_FILE));
    }

    /** Cria ou abre uma base de dados no caminho indicado. */
    public MoveDatabase(Path databaseFile) {
        Objects.requireNonNull(databaseFile, "O caminho da base de dados não pode ser nulo");
        Path absolutePath = databaseFile.toAbsolutePath().normalize();
        Path parent = absolutePath.getParent();

        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível criar a pasta da base de dados", exception);
        }

        this.jdbcUrl = "jdbc:sqlite:" + absolutePath.toString().replace('\\', '/');
        createTables();
    }

    /** Cria um registo para uma nova partida e devolve o seu identificador. */
    public long createGame() {
        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO games DEFAULT VALUES");
            try (ResultSet resultSet = statement.executeQuery("SELECT last_insert_rowid()")) {
                if (resultSet.next()) {
                    return resultSet.getLong(1);
                }
                throw new IllegalStateException("A base de dados não devolveu o identificador da partida");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível registar a partida na base de dados", exception);
        }
    }

    /** Guarda os tiros e resultados de uma jogada numa única transação. */
    public void saveMove(long gameId, IMove move) {
        Objects.requireNonNull(move, "A jogada não pode ser nula");
        List<IPosition> shots = move.getShots();
        List<IGame.ShotResult> results = move.getShotResults();
        if (shots.size() != results.size()) {
            throw new IllegalArgumentException("Cada tiro deve ter exatamente um resultado");
        }

        String sql = "INSERT INTO shots "
                + "(game_id, move_number, shot_number, coordinate, result, ship_type) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = openConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                for (int index = 0; index < shots.size(); index++) {
                    IPosition position = shots.get(index);
                    IGame.ShotResult shotResult = results.get(index);

                    statement.setLong(1, gameId);
                    statement.setInt(2, move.getNumber());
                    statement.setInt(3, index + 1);
                    statement.setString(4, position.getClassicRow() + "" + position.getClassicColumn());
                    statement.setString(5, describeResult(shotResult));
                    if (shotResult.ship() == null) {
                        statement.setNull(6, Types.VARCHAR);
                    } else {
                        statement.setString(6, shotResult.ship().getCategory());
                    }
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível guardar a jogada na base de dados", exception);
        }
    }

    /** Devolve os tiros de uma partida pela ordem das jogadas e dos tiros. */
    public List<StoredShot> findShotsForGame(long gameId) {
        String sql = "SELECT game_id, move_number, shot_number, coordinate, result, ship_type "
                + "FROM shots WHERE game_id = ? ORDER BY move_number, shot_number";
        List<StoredShot> shots = new ArrayList<>();

        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, gameId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    shots.add(new StoredShot(
                            resultSet.getLong("game_id"),
                            resultSet.getInt("move_number"),
                            resultSet.getInt("shot_number"),
                            resultSet.getString("coordinate"),
                            resultSet.getString("result"),
                            resultSet.getString("ship_type")));
                }
            }
            return shots;
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível ler o histórico da base de dados", exception);
        }
    }

    private void createTables() {
        String createGames = "CREATE TABLE IF NOT EXISTS games ("
                + "game_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "started_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)";
        String createShots = "CREATE TABLE IF NOT EXISTS shots ("
                + "game_id INTEGER NOT NULL, "
                + "move_number INTEGER NOT NULL, "
                + "shot_number INTEGER NOT NULL, "
                + "coordinate TEXT NOT NULL, "
                + "result TEXT NOT NULL, "
                + "ship_type TEXT, "
                + "recorded_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "PRIMARY KEY (game_id, move_number, shot_number), "
                + "FOREIGN KEY (game_id) REFERENCES games(game_id) ON DELETE CASCADE)";

        try (Connection connection = openConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createGames);
            statement.executeUpdate(createShots);
        } catch (SQLException exception) {
            throw new IllegalStateException("Não foi possível preparar as tabelas SQLite", exception);
        }
    }

    private Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    private static String describeResult(IGame.ShotResult result) {
        if (!result.valid()) {
            return "FORA_DO_TABULEIRO";
        }
        if (result.repeated()) {
            return "REPETIDO";
        }
        if (result.ship() == null) {
            return "AGUA";
        }
        if (result.sunk()) {
            return "AFUNDADO";
        }
        return "ATINGIDO";
    }

    /** Representa um tiro persistido na base de dados. */
    public record StoredShot(
            long gameId,
            int moveNumber,
            int shotNumber,
            String coordinate,
            String result,
            String shipType) {
    }
}
