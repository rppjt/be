package runrush.be.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 비즈니스 로직에서 발생하는 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e,
            HttpServletRequest request) {

        log.warn("비즈니스 예외 발생: code={}, message={}, path={}",
                e.getErrorCode().getCode(), e.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.ofWithCustomMessageAndPath(
                e.getErrorCode(),
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(errorResponse);
    }

    /**
     * 잘못된 타입의 파라미터가 전달된 경우 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e,
            HttpServletRequest request) {

        String requiredTypeName = e.getRequiredType() != null
                ? e.getRequiredType().getSimpleName()
                : "알 수 없음";

        log.warn("파라미터 타입이 일치하지 않습니다: parameter={}, value={}, requiredType={}",
                e.getName(), e.getValue(), requiredTypeName);

        String errorMessage = String.format("파라미터 '%s'의 값 '%s'이(가) 올바르지 않습니다.",
                e.getName(), e.getValue());

        ErrorResponse errorResponse = ErrorResponse.ofWithCustomMessageAndPath(
                ErrorCode.INVALID_REQUEST,
                errorMessage,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(errorResponse);
    }

    /**
     * 지원하지 않는 HTTP 메서드 요청 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest request) {

        log.warn("지원하지 않는 HTTP 메서드={}, uri={}", request.getMethod(), request.getRequestURI());

        ErrorResponse errorResponse = ErrorResponse.ofWithPath(
                ErrorCode.METHOD_NOT_ALLOWED,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(ErrorCode.METHOD_NOT_ALLOWED.getStatus())
                .body(errorResponse);
    }

    /**
     * 요청 본문을 읽을 수 없는 경우 처리 (잘못된 JSON 등)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e,
            HttpServletRequest request) {

        log.warn("요청 본문 읽기 실패: {}", e.getMessage());

        ErrorResponse errorResponse = ErrorResponse.ofWithCustomMessageAndPath(
                ErrorCode.INVALID_REQUEST,
                "요청 본문을 읽을 수 없습니다. JSON 형식을 확인해주세요.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.getStatus())
                .body(errorResponse);
    }

    /**
     * 404 Not Found 처리 (핸들러를 찾을 수 없는 경우)
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFoundException(
            NoHandlerFoundException e,
            HttpServletRequest request) {

        log.warn("핸들러를 찾을 수 없습니다: method={}, uri={}", e.getHttpMethod(), e.getRequestURL());

        ErrorResponse errorResponse = ErrorResponse.ofWithPath(
                ErrorCode.NOT_FOUND,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(ErrorCode.NOT_FOUND.getStatus())
                .body(errorResponse);
    }

    /**
     * 예상하지 못한 모든 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(
            Exception e,
            HttpServletRequest request) {

        log.error("예상치 못한 예외 발생: ", e);

        ErrorResponse errorResponse = ErrorResponse.ofWithPath(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(errorResponse);
    }
}