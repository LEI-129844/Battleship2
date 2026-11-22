# Refactoring Opportunities

| Local | Cheiro no código | Refabricação | Nº aluno |
|---|---|---|---|
| `ScoreboardManager` (campo `gson`) | Nome de constante fora da convenção (`ConstantName`) | Rename | 111491 |
| `ScoreEntry::ScoreEntry(int, int, boolean)` | Literal mágico (formato da data `"yyyy-MM-dd HH:mm:ss"`) | Introduce Constant | 111491 |
| `ScoreboardManager::carregarScores` | Objeto recriado em cada chamada (`TypeToken` da lista) | Introduce Field | 111491 |
| `Move::processEnemyFire` | Duplicated Code (`x > 1 ? "s" : ""` repetido 10 vezes) | Extract Method | 111491 |
| `CallbackClient::sendShots` | Expressão repetida (`response.statusCode()` 3 vezes) | Introduce Variable | 111491 |
| `CallbackClient::sendShots` | Variável temporária desnecessária (`url`, usada uma só vez) | Inline Variable | 111491 |
| `Fleet::printShips` | Método que não usa o estado do objeto (`MethodMayBeStatic`, proxy de Feature Envy na Tabela 1) | Make Static | 111491 |
| `restserver.GameController::receiveShots` | Long Method (condição de validação comprida) | Extract Method | 129844 |
| `restserver.GameController::receiveShots` | Magic Number (1 e 10) | Introduce Constant | 129844 |
| `restserver.GameController::evaluateShots` | Expressão repetida (sr.ship()) | Introduce Variable | 129844 |
| `restserver.GameController::evaluateShots` | Unused Parameter (session) | Safe Delete | 129844 |
| `battleship.Game::over` | Magic String / Duplicated Code (moldura) | Introduce Constant | 129844 |
| `battleship.Game::readEnemyFire` | Long Method | Extract Method | 129844 |
| `battleship.Game::getRemainingShips` | Variável temporária desnecessária | Inline Variable | 129844 |
