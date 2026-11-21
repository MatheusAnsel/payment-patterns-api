package com.matheusansel.payments.domain;

import com.matheusansel.payments.domain.exception.RegraNegocioException;
import com.matheusansel.payments.domain.strategy.ResultadoProcessamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pagamentos")
public class Pagamento {

    @Id
    private UUID id;

    @Column(name = "pedido_id", nullable = false, length = 64)
    private String pedidoId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPagamento metodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal taxa;

    @Column(name = "valor_final", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorFinal;

    @Column(nullable = false)
    private int parcelas;

    @Column(name = "valor_parcela", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorParcela;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Column(name = "motivo_recusa")
    private String motivoRecusa;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    private long versao;

    protected Pagamento() {
        // exigido pelo JPA
    }

    private Pagamento(String pedidoId, BigDecimal valor, MetodoPagamento metodo,
                      ResultadoProcessamento resultado, Instant agora) {
        this.id = UUID.randomUUID();
        this.pedidoId = pedidoId;
        this.valor = Dinheiro.arredondar(valor);
        this.metodo = metodo;
        this.status = resultado.status();
        this.taxa = resultado.taxa();
        this.valorFinal = resultado.valorFinal();
        this.parcelas = resultado.parcelas();
        this.valorParcela = resultado.valorParcela();
        this.dataVencimento = resultado.dataVencimento();
        this.motivoRecusa = resultado.motivoRecusa();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    public static Pagamento criar(String pedidoId, BigDecimal valor, MetodoPagamento metodo,
                                  ResultadoProcessamento resultado, Instant agora) {
        if (valor == null || valor.signum() <= 0) {
            throw new RegraNegocioException("O valor do pagamento deve ser maior que zero");
        }
        return new Pagamento(pedidoId, valor, metodo, resultado, agora);
    }

    /**
     * Confirma a compensacao de um boleto pendente.
     */
    public void confirmar(Instant agora) {
        if (metodo != MetodoPagamento.BOLETO) {
            throw new RegraNegocioException("Apenas pagamentos via boleto podem ser confirmados");
        }
        if (status != StatusPagamento.PENDENTE) {
            throw new RegraNegocioException("Somente boletos pendentes podem ser confirmados. Status atual: " + status);
        }
        this.status = StatusPagamento.APROVADO;
        this.atualizadoEm = agora;
    }

    public void estornar(Instant agora) {
        if (status != StatusPagamento.APROVADO) {
            throw new RegraNegocioException("Somente pagamentos aprovados podem ser estornados. Status atual: " + status);
        }
        this.status = StatusPagamento.ESTORNADO;
        this.atualizadoEm = agora;
    }

    public UUID getId() {
        return id;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public MetodoPagamento getMetodo() {
        return metodo;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public BigDecimal getTaxa() {
        return taxa;
    }

    public BigDecimal getValorFinal() {
        return valorFinal;
    }

    public int getParcelas() {
        return parcelas;
    }

    public BigDecimal getValorParcela() {
        return valorParcela;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public String getMotivoRecusa() {
        return motivoRecusa;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
