package battleship;

public class LongMethodSmell {

    public String processarJogada(int linha, int coluna, char[][] tabuleiro, int[] navios, boolean jogador1) {
        String resultado = "";
        int acertos = 0;
        int falhas = 0;
        int repetidas = 0;
        int afundados = 0;

        if (tabuleiro == null) {
            return "tabuleiro invalido";
        }
        if (linha < 0 || linha >= tabuleiro.length) {
            return "linha fora do tabuleiro";
        }
        if (coluna < 0 || coluna >= tabuleiro[0].length) {
            return "coluna fora do tabuleiro";
        }

        char celula = tabuleiro[linha][coluna];

        if (celula == 'X' || celula == 'O') {
            repetidas++;
            resultado = "posicao ja jogada";
        } else if (celula == '~') {
            falhas++;
            tabuleiro[linha][coluna] = 'O';
            resultado = "agua";
        } else {
            acertos++;
            tabuleiro[linha][coluna] = 'X';
            resultado = "acertou";
            for (int i = 0; i < navios.length; i++) {
                if (navios[i] > 0 && celula - '0' == i) {
                    navios[i]--;
                    if (navios[i] == 0) {
                        afundados++;
                        resultado = "afundou navio " + i;
                    }
                }
            }
        }

        int naviosRestantes = 0;
        for (int i = 0; i < navios.length; i++) {
            if (navios[i] > 0) {
                naviosRestantes++;
            }
        }

        if (naviosRestantes == 0) {
            resultado = resultado + " | fim do jogo";
        } else if (jogador1 && acertos > 0) {
            resultado = resultado + " | jogador 1 joga outra vez";
        } else if (!jogador1 && acertos > 0) {
            resultado = resultado + " | jogador 2 joga outra vez";
        }

        String resumo = "acertos=" + acertos + ", falhas=" + falhas
                + ", repetidas=" + repetidas + ", afundados=" + afundados;
        System.out.println(resumo);

        return resultado;
    }
}