package battleship;

/**
 * Classe criada de propósito para provocar uma violação do MemberName
 * (Ficha 3, Tarefa 2B.4, LEI-123275). Não deve ser integrada no main.
 */
public final class MemberNameViolation {

    private int Numero_Tiros;

    int getShots() {
        return Numero_Tiros;
    }
}
