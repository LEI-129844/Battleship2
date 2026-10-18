package battleship;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ScoreboardManager {
    private static final String FILE_PATH = "scoreboard.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static List<ScoreEntry> carregarScores() {
        File file = new File(FILE_PATH);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<ScoreEntry>>() {}.getType();
            List<ScoreEntry> scores = gson.fromJson(reader, listType);
            return scores != null ? scores : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Erro ao carregar scoreboard: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void registarPartida(int totalTiros, int naviosAfundados, boolean vitoria) {
        List<ScoreEntry> scores = carregarScores();
        scores.add(new ScoreEntry(totalTiros, naviosAfundados, vitoria));

        try (Writer writer = new FileWriter(FILE_PATH)) {
            gson.toJson(scores, writer);
            System.out.println("Partida guardada com sucesso no ficheiro 'scoreboard.json'!");
        } catch (IOException e) {
            System.err.println("Erro ao gravar no scoreboard: " + e.getMessage());
        }
    }

    public static void imprimirScoreboard() {
        List<ScoreEntry> scores = carregarScores();
        System.out.println("\n===== SCOREBOARD DE JOGOS PASSADOS =====");
        if (scores.isEmpty()) {
            System.out.println("Nenhum jogo registado até ao momento.");
        } else {
            for (int i = 0; i < scores.size(); i++) {
                System.out.println((i + 1) + ". " + scores.get(i));
            }
        }
        System.out.println("========================================\n");
    }
}