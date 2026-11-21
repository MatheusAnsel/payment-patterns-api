package com.matheusansel.payments.api;

import com.matheusansel.payments.api.dto.CriarPagamentoRequest;
import com.matheusansel.payments.api.dto.PagamentoResponse;
import com.matheusansel.payments.api.dto.PaginaResponse;
import com.matheusansel.payments.application.CheckoutFacade;
import com.matheusansel.payments.domain.MetodoPagamento;
import com.matheusansel.payments.domain.Pagamento;
import com.matheusansel.payments.domain.StatusPagamento;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/pagamentos")
@Tag(name = "Pagamentos")
public class PagamentoController {

    private final CheckoutFacade checkout;

    public PagamentoController(CheckoutFacade checkout) {
        this.checkout = checkout;
    }

    @PostMapping
    @Operation(summary = "Cria e processa um pagamento")
    public ResponseEntity<PagamentoResponse> criar(@Valid @RequestBody CriarPagamentoRequest request) {
        Pagamento pagamento = checkout.processar(request.paraComando());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(pagamento.getId()).toUri();
        return ResponseEntity.created(location).body(PagamentoResponse.de(pagamento));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um pagamento pelo id")
    public PagamentoResponse buscar(@PathVariable UUID id) {
        return PagamentoResponse.de(checkout.buscar(id));
    }

    @GetMapping
    @Operation(summary = "Lista pagamentos com filtros opcionais de status e metodo")
    public PaginaResponse<PagamentoResponse> listar(
            @RequestParam(required = false) StatusPagamento status,
            @RequestParam(required = false) MetodoPagamento metodo,
            @ParameterObject @PageableDefault(size = 20, sort = "criadoEm", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return PaginaResponse.de(checkout.listar(status, metodo, pageable), PagamentoResponse::de);
    }

    @PostMapping("/{id}/confirmar")
    @Operation(summary = "Confirma a compensacao de um boleto pendente")
    public PagamentoResponse confirmar(@PathVariable UUID id) {
        return PagamentoResponse.de(checkout.confirmarBoleto(id));
    }

    @PostMapping("/{id}/estornar")
    @Operation(summary = "Estorna um pagamento aprovado")
    public PagamentoResponse estornar(@PathVariable UUID id) {
        return PagamentoResponse.de(checkout.estornar(id));
    }
}
