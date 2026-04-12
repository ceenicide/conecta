INSERT ofertas_futuras          ← vendedor criou oferta
SELECT demandas WHERE produto   ← sistema buscou demandas compatíveis (não havia ainda)

INSERT demandas                 ← comprador criou demanda
SELECT ofertas_futuras WHERE produto AND data <= dataLimite  ← sistema buscou ofertas
SELECT matchings WHERE oferta + demanda (verificou duplicata)
INSERT matchings                ← MATCHING CRIADO AUTOMATICAMENTE

UPDATE matchings SET status = ACEITO
UPDATE ofertas_futuras SET status = FECHADA
UPDATE demandas SET status = ATENDIDA  ← tudo na mesma transação

SELECT ofertas_futuras WHERE status = ABERTA  → lista vazia
SELECT demandas WHERE status = PROCURANDO    → lista vazia
SELECT matchings WHERE oferta_id            → matching ACEITO