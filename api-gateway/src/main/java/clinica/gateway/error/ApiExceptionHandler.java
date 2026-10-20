package clinica.gateway.error;

import clinica.gateway.dto.ErrorResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/** Traduz exceções (gRPC, validação, JSON) para HTTP com corpo {"message"}. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** Tabela da especificação: status gRPC -> status HTTP. */
    @ExceptionHandler(StatusRuntimeException.class)
    ResponseEntity<ErrorResponse> grpc(StatusRuntimeException e) {
        Status s = e.getStatus();
        HttpStatus http = switch (s.getCode()) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case DEADLINE_EXCEEDED -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        String desc = s.getDescription();
        String msg = switch (s.getCode()) {
            // Erros de negócio: a descrição em português vem do serviço (withDescription).
            case INVALID_ARGUMENT, NOT_FOUND, ALREADY_EXISTS -> desc != null ? desc : "Requisição recusada pelo serviço.";
            // UNAVAILABLE pode vir da agenda (pacientes fora do ar, com descrição em português)
            // ou do próprio transporte (descrição técnica em inglês, ex.: "io exception").
            case UNAVAILABLE -> desc != null && !desc.startsWith("io exception") && e.getCause() == null
                    ? desc : "Serviço temporariamente indisponível.";
            case DEADLINE_EXCEEDED -> "O serviço demorou demais para responder.";
            default -> "Erro interno ao processar a requisição.";
        };
        log.warn("gRPC {} -> HTTP {}: {}", s.getCode(), http.value(), desc);
        return erro(http, msg);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validacao(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getAllErrors().stream()
                .map(err -> err.getDefaultMessage()).distinct().sorted()
                .collect(Collectors.joining("; "));
        return erro(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> jsonInvalido(HttpMessageNotReadableException e) {
        return erro(HttpStatus.BAD_REQUEST, "JSON malformado ou com tipos inválidos.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ErrorResponse> parametroAusente(MissingServletRequestParameterException e) {
        return erro(HttpStatus.BAD_REQUEST, "Parâmetro " + e.getParameterName() + " é obrigatório.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> tipoInvalido(MethodArgumentTypeMismatchException e) {
        return erro(HttpStatus.BAD_REQUEST, "Valor inválido para " + e.getName() + ".");
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> http(ResponseStatusException e) {
        String msg = e.getReason() == null ? "Falha na operação." : e.getReason();
        return erro(HttpStatus.valueOf(e.getStatusCode().value()), msg);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErrorResponse> metodo(HttpRequestMethodNotSupportedException e) {
        return erro(HttpStatus.METHOD_NOT_ALLOWED, "Método não suportado nesta rota.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErrorResponse> naoEncontrado(NoResourceFoundException e) {
        return erro(HttpStatus.NOT_FOUND, "Recurso não encontrado.");
    }

    private static ResponseEntity<ErrorResponse> erro(HttpStatus status, String msg) {
        return ResponseEntity.status(status).body(new ErrorResponse(msg));
    }
}
