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
