package runrush.be.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    // ===== 공통 에러 (C: Common) =====
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C002", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C005", "허용되지 않은 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C006", "서버 내부 오류가 발생했습니다."),

    // ===== 사용자 관련 에러 (U: User) =====
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "U002", "이미 존재하는 사용자입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U003", "비밀번호가 올바르지 않습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "U004", "이미 사용 중인 이메일입니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "U005", "올바르지 않은 이메일 형식입니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "U006", "비밀번호가 일치하지 않습니다."),
    USER_DEACTIVATED(HttpStatus.FORBIDDEN, "U007", "비활성화된 사용자입니다."),

    // ===== JWT 관련 에러 (J: JWT) =====
    ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "J001", "Access Token이 만료되었습니다. /auth/token 호출로 새 accessToken을 발급받아 원래 요청을 재시도하세요."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "J002", "Refresh Token이 만료되었습니다. 사용자 로그아웃 후 로그인 페이지로 리디렉션하세요."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "J003", "유효하지 않은 토큰입니다. 로그인 필요 및 로그인 페이지로 이동하세요."),
    TOKEN_NOT_PROVIDED(HttpStatus.UNAUTHORIZED, "J004", "토큰이 제공되지 않았습니다."),
    MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED, "J005", "토큰 형식이 올바르지 않습니다."),
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "J006", "지원되지 않는 토큰입니다."),
    TOKEN_SIGNATURE_INVALID(HttpStatus.UNAUTHORIZED, "J007", "토큰 서명이 유효하지 않습니다."),

    // ===== 친구 관련 에러 (F: Friends) =====
    FRIEND_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "친구 요청을 찾을 수 없습니다."),
    FRIEND_REQUEST_ALREADY_EXISTS(HttpStatus.CONFLICT, "F002", "이미 친구 요청이 존재합니다."),
    CANNOT_ADD_SELF_AS_FRIEND(HttpStatus.BAD_REQUEST, "F003", "자기 자신을 친구로 추가할 수 없습니다."),
    ALREADY_FRIENDS(HttpStatus.CONFLICT, "F004", "이미 친구 관계입니다."),
    FRIEND_REQUEST_ALREADY_PROCESSED(HttpStatus.BAD_REQUEST, "F005", "이미 처리된 친구 요청입니다."),

    // ===== 게시글 관련 에러 (P: Post) =====
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "게시글을 찾을 수 없습니다."),
    POST_ACCESS_DENIED(HttpStatus.FORBIDDEN, "P002", "게시글에 접근할 권한이 없습니다."),
    POST_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "P003", "게시글 제목은 필수입니다."),
    POST_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "P004", "게시글 내용은 필수입니다."),
    POST_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "P005", "게시글 제목이 너무 깁니다."),

    // ===== 댓글 관련 에러 (CM: Comment) =====
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CM001", "댓글을 찾을 수 없습니다."),
    COMMENT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CM002", "댓글에 접근할 권한이 없습니다."),
    COMMENT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "CM003", "댓글 내용은 필수입니다."),
    COMMENT_CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "CM004", "댓글 내용이 너무 깁니다."),

    // ===== 파일 관련 에러 (FL: File) =====
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FL001", "파일을 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FL002", "파일 업로드에 실패했습니다."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "FL003", "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FL004", "파일 크기가 제한을 초과했습니다."),

    // ===== 인증/인가 관련 에러 (A: Auth) =====
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "A001", "로그인에 실패했습니다."),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "A002", "계정이 잠겨있습니다."),
    TOO_MANY_LOGIN_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "A003", "로그인 시도 횟수를 초과했습니다."),
    EMAIL_VERIFICATION_REQUIRED(HttpStatus.FORBIDDEN, "A004", "이메일 인증이 필요합니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "A005", "유효하지 않은 인증 코드입니다."),

    // ===== 위치 관련 에러 (L: Location) =====
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "위치 정보를 찾을 수 없습니다."),
    LOCATION_UPDATE_REQUIRED(HttpStatus.BAD_REQUEST, "L002", "위치 정보 업데이트가 필요합니다."),
    INVALID_COORDINATES(HttpStatus.BAD_REQUEST, "L003", "유효하지 않은 좌표입니다."),
    LOCATION_SHARING_DISABLED(HttpStatus.FORBIDDEN, "L004", "위치 공유가 비활성화되어 있습니다."),
    LOCATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "L005", "위치 정보에 접근할 권한이 없습니다."),
    GPS_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "L006", "GPS 권한이 거부되었습니다."),
    LOCATION_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "L007", "위치 서비스를 사용할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
