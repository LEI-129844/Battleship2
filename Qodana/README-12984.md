# 129844 - Henrique Fernandes — Qodana: Long Method

Cheiro escolhido: **Long Method** (Fowler), formalizado no livro como **Brain Method** (Lanza & Marinescu, pp. 92–93).

## Tabela de mapeamento

| Cheiro (Fowler) | Inspeção no IDE | ID da inspeção | Métrica / limiar | Limitações |
|---|---|---|---|---|
| Long Method | Overly long method | `NonCommentSourceStatements` (suppress: `OverlyLongMethod`) | Statements (NCSS); omissão 30; usado 15 (`m_limit`) | Mede statements, não LOC. |
| Long Method | Overly complex method | `CyclomaticComplexity` (suppress: `OverlyComplexMethod`) | CYCLO; omissão 10; usado 3 (`m_limit`) | Inspeções independentes (comportam-se como OR, não AND). |
| (Brain Method) | — | — | MAXNESTING, NOAV | Sem inspeção equivalente confirmada para NOAV; "Overly nested method" por confirmar. |

## Execuções do quality gate

- Execução que falha: _(ligação)_
- Execução que passa: _(ligação)_

## Reflexão

_(3–4 frases: o que a ferramenta detetou, falsos positivos/negativos, sensibilidade aos limiares, limitações face ao livro.)_
