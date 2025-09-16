package runrush.be.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse (
    String code,
    String message,
    LocalDateTime timestamp,
    String path
)

    {
        public static ErrorResponse of (ErrorCode errorCode){
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now(),
                null
        );
    }

        public static ErrorResponse ofWithCustomMessage (ErrorCode errorCode, String customMessage){
        return new ErrorResponse(
                errorCode.getCode(),
                customMessage,
                LocalDateTime.now(),
                null
        );
    }

        public static ErrorResponse ofWithPath (ErrorCode errorCode, String path){
        return new ErrorResponse(
                errorCode.getCode(),
                errorCode.getMessage(),
                LocalDateTime.now(),
                path
        );
    }

        public static ErrorResponse ofWithCustomMessageAndPath (ErrorCode errorCode, String customMessage, String path){
        return new ErrorResponse(
                errorCode.getCode(),
                customMessage,
                LocalDateTime.now(),
                path
        );
    }
    }