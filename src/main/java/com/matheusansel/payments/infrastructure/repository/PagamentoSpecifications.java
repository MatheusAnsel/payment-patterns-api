package com.matheusansel.payments.infrastructure.repository;

import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.Pagamento;
import com.matheusansel.payments.domain.StatusPagamento;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros opcionais da listagem. Filtro nulo nao restringe nada.
 */
public final class PagamentoSpecifications {

    private PagamentoSpecifications() {
    }

    public static Specification<Pagamento> comFiltros(StatusPagamento status, MetodoPagamento metodo) {
        return Specification.allOf(comStatus(status), comMetodo(metodo));
    }

    private static Specification<Pagamento> comStatus(StatusPagamento status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<Pagamento> comMetodo(MetodoPagamento metodo) {
        return (root, query, cb) -> metodo == null ? null : cb.equal(root.get("metodo"), metodo);
    }
}
