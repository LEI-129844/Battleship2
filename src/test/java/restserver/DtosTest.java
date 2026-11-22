package restserver;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DtosTest {

    @Test
    @DisplayName("Atribui os campos de uma resposta de registo")
    void registrationResponseFields() {
        Dtos.RegistrationResponse response = new Dtos.RegistrationResponse();
        response.gameId = "game-1";
        response.status = "REGISTERED";
        response.shotsPerTurn = 3;
        response.message = "Pronto";

        assertEquals("game-1", response.gameId);
        assertEquals("REGISTERED", response.status);
        assertEquals(3, response.shotsPerTurn);
        assertEquals("Pronto", response.message);
    }

    @Test
    @DisplayName("Atribui os campos de uma posicao e pedido de tiros")
    void shotRequestFields() {
        Dtos.ShotPosition position = new Dtos.ShotPosition();
        position.row = "A";
        position.column = 1;

        Dtos.ShotRequest request = new Dtos.ShotRequest();
        request.shots = List.of(position);

        assertEquals("A", request.shots.get(0).row);
        assertEquals(1, request.shots.get(0).column);
    }

    @Test
    @DisplayName("Atribui os campos de um pedido de registo")
    void registrationRequestFields() {
        Dtos.RegistrationRequest request = new Dtos.RegistrationRequest();
        request.setPlayerName("Alice");
        request.setCallbackUrl("http://localhost:9090");

        assertEquals("Alice", request.getPlayerName());
        assertEquals("http://localhost:9090", request.getCallbackUrl());
    }

    @Test
    @DisplayName("Atribui todos os campos de uma resposta de tiro")
    void shotResponseFields() {
        Dtos.ShotResult shot = new Dtos.ShotResult();
        shot.row = "B";
        shot.column = 3;
        shot.outcome = "SUNK";
        shot.shipType = "galeao";

        Dtos.ShotResponse response = new Dtos.ShotResponse();
        response.results = List.of(shot);
        response.shipsRemaining = 7;
        response.gameStatus = "ONGOING";
        response.winner = null;

        assertEquals(7, response.shipsRemaining);
        assertEquals("ONGOING", response.gameStatus);
        assertNull(response.winner);
        assertEquals("B", response.results.get(0).row);
        assertEquals(3, response.results.get(0).column);
        assertEquals("SUNK", response.results.get(0).outcome);
        assertEquals("galeao", response.results.get(0).shipType);
    }

    @Test
    @DisplayName("Cria uma resposta de erro com a mensagem indicada")
    void errorResponseFields() {
        Dtos.ErrorResponse response = new Dtos.ErrorResponse("Jogo nao encontrado");

        assertEquals("Jogo nao encontrado", response.error);
    }
}
