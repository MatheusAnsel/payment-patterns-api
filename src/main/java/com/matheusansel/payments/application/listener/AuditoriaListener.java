package com.matheusansel.payments.application.listener;

import com.matheusansel.payments.application.evento.PagamentoAprovadoEvent;
import com.matheusansel.payments.application.evento.PagamentoEstornadoEvent;
import com.matheusansel.payments.application.evento.PagamentoRecusadoEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Trilha de auditoria. Roda de forma sincrona dentro da transacao, entao fica registrado
 * inclusive o que acabou sofrendo rollback depois.
 */
@Component
public class AuditoriaListener {

    private static final Logger AUDITORIA = LoggerFactory.getLogger("auditoria.pagamentos");

    @EventListener
    public void aoAprovar(PagamentoAprovadoEvent evento) {
        AUDITORIA.info("APROVADO pagamento={} pedido={} metodo={} valorFinal={}",
                evento.pagamentoId(), evento.pedidoId(), evento.metodo(), evento.valorFinal());
    }

    @EventListener
    public void aoRecusar(PagamentoRecusadoEvent evento) {
        AUDITORIA.warn("RECUSADO pagamento={} pedido={} metodo={} valor={} motivo=\"{}\"",
                evento.pagamentoId(), evento.pedidoId(), evento.metodo(), evento.valor(), evento.motivo());
    }

    @EventListener
    public void aoEstornar(PagamentoEstornadoEvent evento) {
        AUDITORIA.info("ESTORNADO pagamento={} pedido={} valorFinal={}",
                evento.pagamentoId(), evento.pedidoId(), evento.valorFinal());
    }
}
