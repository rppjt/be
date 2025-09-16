package runrush.be.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
     * UNIQUE 제약조건 위반 처리 (동시성 문제 해결)
     * 
     * 사용자의 중복 액션(좋아요, 북마크, 친구 요청)을 우아하게 처리
     */
    @ExceptionHandler({DataIntegrityViolationException.class, DuplicateKeyException.class})
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e,
            HttpServletRequest request) {
            
        String constraintName = extractConstraintName(e);
        ErrorCode errorCode = determineErrorCodeByConstraint(constraintName);
        
        log.warn("🔐 동시성 제어 - 중복 데이터 방지: constraint={}, path={}", 
                constraintName, request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.ofWithPath(
                errorCode,
                request.getRequestURI()
        );
        
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(errorResponse);
    }
    
    /**
     * 낙관적 락 충돌 처리
     * 
     * 여러 사용자가 동시에 같은 데이터를 수정할 때 발생
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailureException(
            ObjectOptimisticLockingFailureException e,
            HttpServletRequest request) {
            
        log.warn("낙관적 락 충돌 발생: class={}, path={}",
                e.getPersistentClassName(), request.getRequestURI());
        
        ErrorResponse errorResponse = ErrorResponse.ofWithPath(
                ErrorCode.OPTIMISTIC_LOCK_ERROR,
                request.getRequestURI()
        );
        
        return ResponseEntity
                .status(ErrorCode.OPTIMISTIC_LOCK_ERROR.getStatus())
                .body(errorResponse);
    }
    
    /**
     * 제약조건 이름을 기반으로 적절한 ErrorCode 결정
     */
    private ErrorCode determineErrorCodeByConstraint(String constraintName) {
        if (constraintName.contains("course_like")) {
            return ErrorCode.DUPLICATE_COURSE_LIKE;
        } else if (constraintName.contains("course_bookmark")) {
            return ErrorCode.DUPLICATE_COURSE_BOOKMARK;
        } else if (constraintName.contains("friends")) {
            return ErrorCode.DUPLICATE_FRIEND_REQUEST;
        }
        return ErrorCode.CONCURRENT_MODIFICATION_ERROR;
    }
    
    /**
     * 예외 메시지에서 제약조건 이름 추출
     */
    private String extractConstraintName(Exception e) {
        String message = e.getMessage();
        if (message.contains("'")) {
            // 'uk_course_like_user_course' 형태에서 제약조건 이름 추출
            int start = message.indexOf("'");
            int end = message.indexOf("'", start + 1);
            if (start > -1 && end > start) {
                return message.substring(start + 1, end);
            }
        }
        return "unknown";
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