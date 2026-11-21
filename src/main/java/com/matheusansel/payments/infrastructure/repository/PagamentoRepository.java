package com.matheusansel.payments.infrastructure.repository;

import com.matheusansel.payments.domain.Pagamento;
import com.matheusansel.payments.domain.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID>, JpaSpecificationExecutor<Pagamento> {

    boolean existsByPedidoIdAndStatusIn(String pedidoId, Collection<StatusPagamento> status);
}
