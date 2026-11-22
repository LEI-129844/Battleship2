package battleship;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScoreEntry {
    private static final String FORMATO_DATA = "yyyy-MM-dd HH:mm:ss";
    private String data;
    private int totalTiros;
    private int naviosAfundados;
    private boolean vitoria;

    public ScoreEntry() {}

    public ScoreEntry(int totalTiros, int naviosAfundados, boolean vitoria) {
        this.data = LocalDateTime.now().format(DateTimeFormatter.ofPattern(FORMATO_DATA));
        this.totalTiros = totalTiros;
        this.naviosAfundados = naviosAfundados;
        this.vitoria = vitoria;
    }

    public String getData() { return data; }
    public int getTotalTiros() { return totalTiros; }
    public int getNaviosAfundados() { return naviosAfundados; }
    public boolean isVitoria() { return vitoria; }

    @Override
    public String toString() {
        return String.format("[%s] Resultado: %s | Tiros Disparados: %d | Navios Afundados: %d",
                data, (vitoria ? "VITÓRIA" : "DERROTA"), totalTiros, naviosAfundados);
    }
}