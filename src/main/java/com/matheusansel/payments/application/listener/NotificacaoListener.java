package com.matheusansel.payments.application.listener;

import com.matheusansel.payments.application.evento.PagamentoAprovadoEvent;
import com.matheusansel.payments.application.evento.PagamentoEstornadoEvent;
import com.matheusansel.payments.application.evento.PagamentoRecusadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notificacao ao cliente (simulada com log). So dispara depois do commit: nao faz sentido
 * avisar que um pagamento foi aprovado se a gravacao no banco falhar em seguida.
 * Em producao, aqui entraria um envio de e-mail, push ou publicacao em fila.
 */
@Component
public class NotificacaoListener {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacaoListener.class);

    @TransactionalEventListener
    public void aoAprovar(PagamentoAprovadoEvent evento) {
        LOG.info("[notificacao] Pedido {}: pagamento aprovado (R$ {})", evento.pedidoId(), evento.valorFinal());
    }

    @TransactionalEventListener
    public void aoRecusar(PagamentoRecusadoEvent evento) {
        LOG.info("[notificacao] Pedido {}: pagamento recusado. Motivo: {}", evento.pedidoId(), evento.motivo());
    }

    @TransactionalEventListener
    public void aoEstornar(PagamentoEstornadoEvent evento) {
        LOG.info("[notificacao] Pedido {}: estorno de R$ {} realizado", evento.pedidoId(), evento.valorFinal());
    }
}
