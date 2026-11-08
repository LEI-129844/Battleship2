package battleship;// 129844 - Henrique Fernandes
// Violação DELIBERADA da regra MethodName (passo 4 da parte B).

public class MethodNameViolation {

    // Viola o padrão ^[a-z][a-zA-Z0-9]*$ (começa por maiúscula)
    public int calcularPontos(int acertos, int falhas) {
        return acertos * 10 - falhas;
    }

    // Para corrigir, renomeia para: calcularPontos
}
