CREATE TABLE pagamentos (
    id              UUID           PRIMARY KEY,
    pedido_id       VARCHAR(64)    NOT NULL,
    valor           NUMERIC(15, 2) NOT NULL CHECK (valor > 0),
    metodo          VARCHAR(20)    NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    taxa            NUMERIC(15, 2) NOT NULL,
    valor_final     NUMERIC(15, 2) NOT NULL,
    parcelas        INTEGER        NOT NULL,
    valor_parcela   NUMERIC(15, 2) NOT NULL,
    data_vencimento DATE,
    motivo_recusa   VARCHAR(255),
    criado_em       TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em   TIMESTAMP WITH TIME ZONE NOT NULL,
    versao          BIGINT         NOT NULL
);

CREATE INDEX idx_pagamentos_pedido_status ON pagamentos (pedido_id, status);
CREATE INDEX idx_pagamentos_status_metodo ON pagamentos (status, metodo);
CREATE INDEX idx_pagamentos_criado_em ON pagamentos (criado_em);
