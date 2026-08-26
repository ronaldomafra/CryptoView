# Plano — informações da moeda via CoinPaprika

Plano aprovado em 26/08/2026 e implementado no mesmo marco.

## Objetivo

Antecipar a resolução segura do identificador CoinPaprika ao expandir uma moeda e disponibilizar uma tela de informações descritivas sem compartilhar ou exigir a API key da CoinMarketCap.

## Fluxo aprovado

1. A expansão inicia normalmente cotação, corretoras, histórico e polling CoinMarketCap.
2. Em paralelo, o app consulta `/v1/search` somente quando ainda não existe um mapeamento local.
3. A busca limita a categoria a moedas, usa `symbol_search` e aceita apenas uma correspondência ativa com símbolo exato e nome ou slug compatível.
4. O botão `Informações` permanece desabilitado até o fim da carga principal CoinMarketCap e até existir um `coin_id` seguro.
5. A tela recebe o ID já resolvido e consulta somente `/v1/coins/{coin_id}`; ela nunca repete a busca.

## Persistência e cache

- `coin_paprika_mapping` preserva a relação entre o ID CoinMarketCap local e o ID CoinPaprika.
- `coin_paprika_info` guarda os dados essenciais da tela por 24 horas.
- Um `404` invalida o mapeamento, sem iniciar uma segunda chamada automática na tela.
- A integração é sob demanda e não participa da sincronização principal.

## Conteúdo da tela

- Identidade, ranking, tipo e status.
- Descrição e mensagem relevante.
- Lançamento, desenvolvimento, código aberto e hardware wallet.
- Consenso, algoritmo de hash e estrutura organizacional.
- Website, explorador, código-fonte e whitepaper.

## Critérios de segurança

- Nenhum header autenticado é enviado à CoinPaprika.
- Símbolos parciais, moedas inativas e resultados ambíguos são recusados.
- Ranking não é utilizado como desempate.
- Apenas links HTTP/HTTPS são apresentados como ações externas.
