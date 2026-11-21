package com.matheusansel.payments.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matheusansel.payments.application.evento.PagamentoAprovadoEvent;
import com.matheusansel.payments.application.evento.PagamentoEstornadoEvent;
import com.matheusansel.payments.application.evento.PagamentoRecusadoEvent;
import com.matheusansel.payments.infrastructure.repository.PagamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class PagamentoControllerIntegrationTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    /**
     * Quarta-feira, 14h em Brasilia: fora da janela antifraude de madrugada e com vencimento
     * de boleto previsivel (segunda-feira seguinte).
     */
    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock relogioDeTeste() {
            return Clock.fixed(LocalDateTime.of(2026, 11, 18, 14, 0).atZone(SP).toInstant(), SP);
        }
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private PagamentoRepository repository;
    @Autowired
    private ApplicationEvents eventos;

    @BeforeEach
    void limparBase() {
        repository.deleteAll();
    }

    private ResultActions criar(String pedido, String valor, String metodo, Integer parcelas) throws Exception {
        String corpo = """
                {"pedidoId": "%s", "valor": %s, "metodo": "%s"%s}
                """.formatted(pedido, valor, metodo, parcelas == null ? "" : ", \"parcelas\": " + parcelas);
        return mvc.perform(post("/api/pagamentos").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    private String criarERetornarId(String pedido, String valor, String metodo) throws Exception {
        String resposta = criar(pedido, valor, metodo, null).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(resposta).get("id").asText();
    }

    @Test
    void criaPagamentoPixAprovado() throws Exception {
        criar("PED-100", "199.90", "PIX", null)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/pagamentos/")))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.taxa").value(0.0))
                .andExpect(jsonPath("$.valorFinal").value(199.90))
                .andExpect(jsonPath("$.versao").doesNotExist());

        assertThat(eventos.stream(PagamentoAprovadoEvent.class)).hasSize(1);
    }

    @Test
    void criaPagamentoNoCartaoParceladoComJuros() throws Exception {
        criar("PED-101", "1000.00", "CARTAO_CREDITO", 6)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.taxa").value(34.90))
                .andExpect(jsonPath("$.parcelas").value(6))
                .andExpect(jsonPath("$.valorParcela").value(184.69))
                .andExpect(jsonPath("$.valorFinal").value(1108.14));
    }

    @Test
    void cartaoAcimaDoLimiteFicaRegistradoComoRecusado() throws Exception {
        criar("PED-102", "12000.00", "CARTAO_CREDITO", 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECUSADO"))
                .andExpect(jsonPath("$.motivoRecusa").exists());

        assertThat(eventos.stream(PagamentoRecusadoEvent.class)).hasSize(1);
    }

    @Test
    void boletoFicaPendenteEDepoisPodeSerConfirmado() throws Exception {
        String id = criar("PED-103", "300.00", "BOLETO", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.taxa").value(2.50))
                .andExpect(jsonPath("$.valorFinal").value(302.50))
                .andExpect(jsonPath("$.dataVencimento").value("2026-11-23"))
                .andReturn().getResponse().getContentAsString();
        String pagamentoId = json.readTree(id).get("id").asText();
        assertThat(eventos.stream(PagamentoAprovadoEvent.class)).isEmpty();

        mvc.perform(post("/api/pagamentos/{id}/confirmar", pagamentoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"));
        assertThat(eventos.stream(PagamentoAprovadoEvent.class)).hasSize(1);

        mvc.perform(post("/api/pagamentos/{id}/confirmar", pagamentoId))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void naoConfirmaPagamentoQueNaoEBoleto() throws Exception {
        String id = criarERetornarId("PED-104", "50.00", "PIX");

        mvc.perform(post("/api/pagamentos/{id}/confirmar", id))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"));
    }

    @Test
    void estornaPagamentoAprovadoUmaUnicaVez() throws Exception {
        String id = criarERetornarId("PED-105", "50.00", "PIX");

        mvc.perform(post("/api/pagamentos/{id}/estornar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ESTORNADO"));
        assertThat(eventos.stream(PagamentoEstornadoEvent.class)).hasSize(1);

        mvc.perform(post("/api/pagamentos/{id}/estornar", id))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void estornoLiberaOPedidoParaNovoPagamento() throws Exception {
        String id = criarERetornarId("PED-106", "50.00", "PIX");
        criar("PED-106", "50.00", "PIX", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.regra").value("PEDIDO_DUPLICADO"));

        mvc.perform(post("/api/pagamentos/{id}/estornar", id)).andExpect(status().isOk());

        criar("PED-106", "50.00", "PIX", null).andExpect(status().isCreated());
    }

    @Test
    void buscaPorId() throws Exception {
        String id = criarERetornarId("PED-107", "75.00", "PIX");

        mvc.perform(get("/api/pagamentos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.pedidoId").value("PED-107"));
    }

    @Test
    void retorna404ParaPagamentoInexistente() throws Exception {
        mvc.perform(get("/api/pagamentos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void retorna400ParaIdMalformado() throws Exception {
        mvc.perform(get("/api/pagamentos/nao-e-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void listaComFiltrosEPaginacao() throws Exception {
        criarERetornarId("PED-200", "10.00", "PIX");
        criarERetornarId("PED-201", "20.00", "PIX");
        criarERetornarId("PED-202", "30.00", "BOLETO");
        criar("PED-203", "40.00", "CARTAO_CREDITO", 2).andExpect(status().isCreated());

        mvc.perform(get("/api/pagamentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(4));

        mvc.perform(get("/api/pagamentos").param("metodo", "PIX"))
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.conteudo[0].metodo").value("PIX"));

        mvc.perform(get("/api/pagamentos").param("status", "PENDENTE"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].metodo").value("BOLETO"));

        mvc.perform(get("/api/pagamentos").param("status", "APROVADO").param("metodo", "CARTAO_CREDITO"))
                .andExpect(jsonPath("$.totalElementos").value(1));

        mvc.perform(get("/api/pagamentos").param("size", "3").param("page", "1"))
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.tamanho").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void filtroComValorInvalidoRetorna400() throws Exception {
        mvc.perform(get("/api/pagamentos").param("status", "QUALQUER"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validaCamposObrigatoriosEValorPositivo() throws Exception {
        String resposta = mvc.perform(post("/api/pagamentos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pedidoId\": \"\", \"valor\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andReturn().getResponse().getContentAsString();

        JsonNode campos = json.readTree(resposta).get("campos");
        assertThat(campos.has("pedidoId")).isTrue();
        assertThat(campos.has("valor")).isTrue();
        assertThat(campos.has("metodo")).isTrue();
    }

    @Test
    void metodoDesconhecidoRetorna400() throws Exception {
        criar("PED-300", "10.00", "CHEQUE", null).andExpect(status().isBadRequest());
    }

    @Test
    void parcelamentoForaDoPermitidoNoPixRetorna422() throws Exception {
        criar("PED-301", "100.00", "PIX", 3)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Pix não aceita parcelamento"));
    }

    @Test
    void antifraudeBloqueiaValorAcimaDoTetoComProblemDetail() throws Exception {
        criar("PED-302", "60000.00", "PIX", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://payment-patterns-api/erros/pagamento-bloqueado"))
                .andExpect(jsonPath("$.title").value("Pagamento bloqueado pelo antifraude"))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.regra").value("VALOR_MAXIMO"));

        assertThat(repository.count()).isZero();
    }

    @Test
    void swaggerDisponivel() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/pagamentos']").exists());
    }
}
