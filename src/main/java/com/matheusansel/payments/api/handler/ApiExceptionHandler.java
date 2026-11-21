package com.matheusansel.payments.api.handler;

import com.matheusansel.payments.domain.exception.PagamentoBloqueadoException;
import com.matheusansel.payments.domain.exception.PagamentoNaoEncontradoException;
import com.matheusansel.payments.domain.exception.RegraNegocioException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Todas as respostas de erro seguem a RFC 7807 (application/problem+json).
 * - 400: requisicao malformada ou campos invalidos (Bean Validation)
 * - 404: pagamento inexistente
 * - 409: alteracao concorrente do mesmo pagamento
 * - 422: requisicao valida, mas que viola regra de negocio
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String BASE_TIPO = "https://payment-patterns-api/erros/";

    @ExceptionHandler(PagamentoBloqueadoException.class)
    public ProblemDetail bloqueado(PagamentoBloqueadoException ex) {
        ProblemDetail problema = problema(HttpStatus.UNPROCESSABLE_ENTITY, "pagamento-bloqueado",
                "Pagamento bloqueado pelo antifraude", ex.getMessage());
        problema.setProperty("regra", ex.getRegra());
        return problema;
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ProblemDetail regraNegocio(RegraNegocioException ex) {
        return problema(HttpStatus.UNPROCESSABLE_ENTITY, "regra-negocio", "Regra de negocio violada", ex.getMessage());
    }

    @ExceptionHandler(PagamentoNaoEncontradoException.class)
    public ProblemDetail naoEncontrado(PagamentoNaoEncontradoException ex) {
        return problema(HttpStatus.NOT_FOUND, "nao-encontrado", "Pagamento nao encontrado", ex.getMessage());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail conflito(OptimisticLockingFailureException ex) {
        return problema(HttpStatus.CONFLICT, "conflito", "Conflito de atualizacao",
                "O pagamento foi alterado por outra requisicao. Consulte o estado atual e tente novamente.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail parametroInvalido(MethodArgumentTypeMismatchException ex) {
        return problema(HttpStatus.BAD_REQUEST, "parametro-invalido", "Parametro invalido",
                "Valor invalido para o parametro '" + ex.getName() + "'");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(@NonNull MethodArgumentNotValidException ex,
                                                                  @NonNull HttpHeaders headers,
                                                                  @NonNull HttpStatusCode status,
                                                                  @NonNull WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        ProblemDetail problema = problema(HttpStatus.BAD_REQUEST, "validacao", "Dados invalidos",
                "Um ou mais campos estao invalidos");
        problema.setProperty("campos", campos);
        return ResponseEntity.badRequest().body(problema);
    }

    private ProblemDetail problema(HttpStatus status, String tipo, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setType(URI.create(BASE_TIPO + tipo));
        problema.setTitle(titulo);
        return problema;
    }
}
