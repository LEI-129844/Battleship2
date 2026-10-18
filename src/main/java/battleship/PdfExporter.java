package battleship;

import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Exporta o histórico de jogadas de um jogo de Batalha Naval para um ficheiro PDF,
 * usando a biblioteca OpenPDF (obtida no Maven Central).
 * <p>
 * O relatório inclui:
 * <ul>
 *     <li>data/hora da exportação e estatísticas gerais do jogo;</li>
 *     <li>uma tabela com todas as rajadas (nº, tiros disparados e resultado);</li>
 *     <li>o tabuleiro com a frota e os tiros recebidos.</li>
 * </ul>
 */
public final class PdfExporter {

    /** Pasta onde os relatórios PDF são gravados. */
    public static final String REPORTS_DIR = "reports";

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter HUMAN_TS = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(0, 51, 102));
    private static final Font SECTION_FONT = new Font(Font.HELVETICA, 13, Font.BOLD, new Color(0, 51, 102));
    private static final Font TEXT_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font BOARD_FONT = new Font(Font.COURIER, 10, Font.BOLD);

    private static final Color HEADER_BG = new Color(0, 51, 102);
    private static final Color WATER_BG = new Color(204, 229, 255);
    private static final Color SHIP_BG = new Color(160, 160, 160);
    private static final Color HIT_BG = new Color(220, 53, 69);
    private static final Color MISS_BG = new Color(255, 255, 255);
    private static final Color ADJACENT_BG = new Color(230, 240, 250);

    private PdfExporter() {
    }

    /**
     * Gera o PDF do jogo na pasta {@value #REPORTS_DIR}, com o nome {@code jogo_<data>.pdf}.
     *
     * @param game o jogo a exportar (não pode ser nulo)
     * @return o caminho do ficheiro PDF criado
     * @throws IOException se não for possível criar a pasta ou o ficheiro
     */
    public static Path export(IGame game) throws IOException {
        assert game != null;

        Path dir = Path.of(REPORTS_DIR);
        Files.createDirectories(dir);
        Path file = dir.resolve("jogo_" + LocalDateTime.now().format(FILE_TS) + ".pdf");

        try (OutputStream out = new FileOutputStream(file.toFile())) {
            export(game, out);
        }
        return file;
    }

    /**
     * Escreve o relatório PDF do jogo para o {@link OutputStream} indicado.
     *
     * @param game o jogo a exportar
     * @param out  destino do PDF
     */
    public static void export(IGame game, OutputStream out) {
        assert game != null;
        assert out != null;

        Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph title = new Paragraph("Batalha Naval - Histórico de Jogadas", TITLE_FONT);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6);
            doc.add(title);

            Paragraph date = new Paragraph("Exportado em " + LocalDateTime.now().format(HUMAN_TS), TEXT_FONT);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(14);
            doc.add(date);

            addSummary(doc, game);
            addMovesTable(doc, game.getAlienMoves());
            addBoard(doc, game);
        } catch (DocumentException e) {
            throw new IllegalStateException("Erro ao gerar o PDF das jogadas", e);
        } finally {
            if (doc.isOpen())
                doc.close();
        }
    }

    /** Secção com as estatísticas gerais do jogo. */
    private static void addSummary(Document doc, IGame game) {
        doc.add(new Paragraph("Resumo", SECTION_FONT));

        int moves = game.getAlienMoves().size();
        String text = "Rajadas disparadas: " + moves
                + "   |   Tiros: " + moves * Game.NUMBER_SHOTS
                + "   |   Acertos: " + game.getHits()
                + "   |   Navios afundados: " + game.getSunkShips()
                + "   |   Navios a flutuar: " + game.getRemainingShips()
                + "\nTiros repetidos: " + game.getRepeatedShots()
                + "   |   Tiros fora do tabuleiro: " + game.getInvalidShots();
        Paragraph p = new Paragraph(text, TEXT_FONT);
        p.setSpacingAfter(12);
        doc.add(p);
    }

    /** Tabela com uma linha por rajada: número, tiros e resultado. */
    private static void addMovesTable(Document doc, List<IMove> moves) {
        doc.add(new Paragraph("Rajadas", SECTION_FONT));

        if (moves.isEmpty()) {
            doc.add(new Paragraph("Ainda não foi disparada nenhuma rajada.", TEXT_FONT));
            doc.add(Chunk.NEWLINE);
            return;
        }

        PdfPTable table = new PdfPTable(new float[]{1.2f, 2.5f, 6.3f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setHeaderRows(1);

        for (String header : new String[]{"Rajada", "Tiros", "Resultado"})
            table.addCell(headerCell(header));

        for (IMove move : moves) {
            table.addCell(textCell(String.valueOf(move.getNumber()), Element.ALIGN_CENTER));
            table.addCell(textCell(shotsToText(move.getShots()), Element.ALIGN_LEFT));
            table.addCell(textCell(describeResults(move.getShotResults()), Element.ALIGN_LEFT));
        }
        doc.add(table);
        doc.add(Chunk.NEWLINE);
    }

    /** Desenha o tabuleiro da frota com os tiros recebidos, como uma grelha colorida. */
    private static void addBoard(Document doc, IGame game) {
        doc.add(new Paragraph("Tabuleiro final", SECTION_FONT));

        char[][] map = buildMap(game.getMyFleet(), game.getAlienMoves());

        PdfPTable board = new PdfPTable(Game.BOARD_SIZE + 1);
        board.setWidthPercentage(60);
        board.setSpacingBefore(6);

        board.addCell(boardCell(" ", HEADER_BG, Color.WHITE));
        for (int c = 0; c < Game.BOARD_SIZE; c++)
            board.addCell(boardCell(String.valueOf(c + 1), HEADER_BG, Color.WHITE));

        for (int r = 0; r < Game.BOARD_SIZE; r++) {
            board.addCell(boardCell(String.valueOf((char) ('A' + r)), HEADER_BG, Color.WHITE));
            for (int c = 0; c < Game.BOARD_SIZE; c++) {
                char m = map[r][c];
                board.addCell(boardCell(String.valueOf(m), colorFor(m), Color.BLACK));
            }
        }
        doc.add(board);

        Paragraph legend = new Paragraph(
                "Legenda:  # navio   * tiro certeiro   o tiro na água   - adjacente a navio afundado   . água",
                TEXT_FONT);
        legend.setSpacingBefore(6);
        doc.add(legend);
    }

    /**
     * Constrói a matriz do tabuleiro com os mesmos símbolos usados na consola
     * (ver {@link Game#printBoard}).
     */
    static char[][] buildMap(IFleet fleet, List<IMove> moves) {
        char[][] map = new char[Game.BOARD_SIZE][Game.BOARD_SIZE];
        for (char[] row : map)
            java.util.Arrays.fill(row, '.');

        for (IShip ship : fleet.getShips()) {
            if (!ship.stillFloating())
                for (IPosition adj : ship.getAdjacentPositions())
                    if (adj.isInside())
                        map[adj.getRow()][adj.getColumn()] = '-';
        }
        for (IShip ship : fleet.getShips())
            for (IPosition pos : ship.getPositions())
                map[pos.getRow()][pos.getColumn()] = '#';

        for (IMove move : moves)
            for (IPosition shot : move.getShots())
                if (shot.isInside()) {
                    char cur = map[shot.getRow()][shot.getColumn()];
                    map[shot.getRow()][shot.getColumn()] = (cur == '#' || cur == '*') ? '*' : 'o';
                }
        return map;
    }

    /** Converte a lista de tiros em texto, ex.: "A5, C10, F5". */
    static String shotsToText(List<IPosition> shots) {
        StringBuilder sb = new StringBuilder();
        for (IPosition shot : shots) {
            if (!sb.isEmpty())
                sb.append(", ");
            sb.append(shot);
        }
        return sb.toString();
    }

    /**
     * Descreve em português o resultado de uma rajada,
     * ex.: "1 Barca ao fundo, 1 tiro em Nau, 1 tiro na água".
     */
    static String describeResults(List<IGame.ShotResult> results) {
        int missed = 0, repeated = 0, outside = 0;
        Map<String, Integer> sunk = new LinkedHashMap<>();
        Map<String, Integer> hits = new LinkedHashMap<>();

        for (IGame.ShotResult r : results) {
            if (!r.valid())
                outside++;
            else if (r.repeated())
                repeated++;
            else if (r.ship() == null)
                missed++;
            else if (r.sunk())
                sunk.merge(r.ship().getCategory(), 1, Integer::sum);
            else
                hits.merge(r.ship().getCategory(), 1, Integer::sum);
        }

        StringBuilder sb = new StringBuilder();
        sunk.forEach((type, n) -> append(sb, n + " " + type + " ao fundo"));
        hits.forEach((type, n) -> append(sb, n + (n > 1 ? " tiros em " : " tiro em ") + type));
        if (missed > 0)
            append(sb, missed + (missed > 1 ? " tiros na água" : " tiro na água"));
        if (repeated > 0)
            append(sb, repeated + (repeated > 1 ? " tiros repetidos" : " tiro repetido"));
        if (outside > 0)
            append(sb, outside + (outside > 1 ? " tiros fora do tabuleiro" : " tiro fora do tabuleiro"));
        return sb.toString();
    }

    private static void append(StringBuilder sb, String part) {
        if (!sb.isEmpty())
            sb.append(", ");
        sb.append(part);
    }

    private static Color colorFor(char marker) {
        return switch (marker) {
            case '#' -> SHIP_BG;
            case '*' -> HIT_BG;
            case 'o' -> MISS_BG;
            case '-' -> ADJACENT_BG;
            default -> WATER_BG;
        };
    }

    private static PdfPCell headerCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        return cell;
    }

    private static PdfPCell textCell(String text, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TEXT_FONT));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        return cell;
    }

    private static PdfPCell boardCell(String text, Color background, Color textColor) {
        Font font = new Font(BOARD_FONT);
        font.setColor(textColor);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setMinimumHeight(18);
        return cell;
    }
}