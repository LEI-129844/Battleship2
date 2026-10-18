package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes da exportação do histórico de jogadas para PDF.
 */
class PdfExporterTest {

    private Game newGameWithOneBarge() {
        Fleet fleet = new Fleet();
        fleet.addShip(new Barge(Compass.NORTH, new Position(0, 0)));
        return new Game(fleet);
    }

    @Test
    @DisplayName("Converte a lista de tiros em texto")
    void shotsToText() {
        List<IPosition> shots = List.of(new Position(0, 4), new Position(2, 9), new Position(5, 4));
        assertEquals("A5, C10, F5", PdfExporter.shotsToText(shots));
    }

    @Test
    @DisplayName("Descreve o resultado de uma rajada em português")
    void describeResults() {
        IShip barca = new Barge(Compass.NORTH, new Position(0, 0));
        List<IGame.ShotResult> results = List.of(
                new IGame.ShotResult(true, false, barca, true),
                new IGame.ShotResult(true, false, null, false),
                new IGame.ShotResult(false, false, null, false));

        assertEquals("1 Barca ao fundo, 1 tiro na água, 1 tiro fora do tabuleiro",
                PdfExporter.describeResults(results));
    }

    @Test
    @DisplayName("O tabuleiro marca acertos e tiros na água")
    void buildMap() {
        Game game = newGameWithOneBarge();
        game.fireShots(List.of(new Position(0, 0), new Position(5, 5), new Position(9, 9)));

        char[][] map = PdfExporter.buildMap(game.getMyFleet(), game.getAlienMoves());
        assertEquals('*', map[0][0]);
        assertEquals('o', map[5][5]);
        assertEquals('-', map[1][1]);
        assertEquals('.', map[3][3]);
    }

    @Test
    @DisplayName("Gera um documento PDF válido")
    void exportProducesPdf() {
        Game game = newGameWithOneBarge();
        game.fireShots(List.of(new Position(0, 0), new Position(5, 5), new Position(9, 9)));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfExporter.export(game, out);

        byte[] pdf = out.toByteArray();
        assertTrue(pdf.length > 1000, "O PDF não deve estar vazio");
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));
    }
}