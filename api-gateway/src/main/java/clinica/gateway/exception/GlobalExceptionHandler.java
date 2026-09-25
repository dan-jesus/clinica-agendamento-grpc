package clinica.gateway.exception;

import clinica.gateway.dto.ErrorResponse;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ErrorResponse(
                Instant.now(), 400, "Bad Request", "Payload inválido.", campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<ErrorResponse> jsonInvalido(Exception ex) {
        return ResponseEntity.badRequest().body(
                ErrorResponse.simples(400, "Bad Request", "JSON, data ou parâmetro em formato inválido."));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> negocio(BusinessException ex) {
        return ResponseEntity.badRequest().body(
                ErrorResponse.simples(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    ResponseEntity<ErrorResponse> naoAutorizado(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ErrorResponse.simples(401, "Unauthorized", ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ErrorResponse> naoEncontrado(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ErrorResponse.simples(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(StatusRuntimeException.class)
    ResponseEntity<ErrorResponse> grpc(StatusRuntimeException ex) {
        if (ex.getStatus().getCode() == Status.Code.INVALID_ARGUMENT) {
            return ResponseEntity.badRequest().body(
                    ErrorResponse.simples(400, "Bad Request", descricao(ex, "Requisição gRPC inválida.")));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                ErrorResponse.simples(503, "Service Unavailable",
                        "Um microsserviço interno não respondeu. " + descricao(ex, "")));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> inesperado(Exception ex) {
        ex.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ErrorResponse.simples(500, "Internal Server Error", "Erro interno no API Gateway."));
    }

    private static String descricao(StatusRuntimeException ex, String padrao) {
        String descricao = ex.getStatus().getDescription();
        return descricao == null || descricao.isBlank() ? padrao : descricao;
    }
}
