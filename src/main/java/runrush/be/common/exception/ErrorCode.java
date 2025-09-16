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

    // ===== 러닝 기록 관련 에러 (RR: Running Record) =====
    RUNNING_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RR001", "러닝 기록을 찾을 수 없습니다."),
    RUNNING_RECORD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "RR002", "러닝 기록에 접근할 권한이 없습니다."),
    RUNNING_RECORD_ALREADY_DELETED(HttpStatus.BAD_REQUEST, "RR003", "이미 삭제된 러닝 기록입니다."),
    RUNNING_RECORD_RESTORE_FAILED(HttpStatus.BAD_REQUEST, "RR004", "복구할 수 있는 러닝 기록이 없습니다."),
    RUNNING_RECORD_DELETE_FAILED(HttpStatus.BAD_REQUEST, "RR005", "영구 삭제할 수 있는 러닝 기록이 없습니다."),
    INVALID_RUNNING_TIME(HttpStatus.BAD_REQUEST, "RR006", "유효하지 않은 러닝 시간입니다."),
    INVALID_PATH_DATA(HttpStatus.BAD_REQUEST, "RR007", "유효하지 않은 경로 데이터입니다."),

    // ===== 추천 코스 관련 에러 (RC: Recommended Course) =====
    RECOMMENDED_COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "RC001", "추천 코스를 찾을 수 없습니다."),
    RECOMMENDED_COURSE_ALREADY_EXISTS(HttpStatus.CONFLICT, "RC002", "이미 추천된 기록입니다."),
    RECOMMENDED_COURSE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "RC003", "추천 코스에 접근할 권한이 없습니다."),

    // ===== 통계 관련 에러 (ST: Statistics) 신규 =====
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "ST001", "유효하지 않은 날짜 범위입니다."),
    STATS_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "ST002", "통계 데이터가 없습니다."),

    // ===== 파일 관련 에러 (FL: File) =====
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FL001", "파일을 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FL002", "파일 업로드에 실패했습니다."),
    INVALID_FILE_FORMAT(HttpStatus.BAD_REQUEST, "FL003", "지원하지 않는 파일 형식입니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "FL004", "파일 크기가 제한을 초과했습니다."),

    // ===== 위치 관련 에러 (L: Location) =====
    LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "위치 정보를 찾을 수 없습니다."),
    LOCATION_UPDATE_REQUIRED(HttpStatus.BAD_REQUEST, "L002", "위치 정보 업데이트가 필요합니다."),
    INVALID_COORDINATES(HttpStatus.BAD_REQUEST, "L003", "유효하지 않은 좌표입니다."),
    LOCATION_SHARING_DISABLED(HttpStatus.FORBIDDEN, "L004", "위치 공유가 비활성화되어 있습니다."),
    LOCATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "L005", "위치 정보에 접근할 권한이 없습니다."),
    GPS_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "L006", "GPS 권한이 거부되었습니다."),
    LOCATION_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "L007", "위치 서비스를 사용할 수 없습니다."),

    // ===== 동시성/중복 관련 에러 (CO: Concurrency) =====
    DUPLICATE_COURSE_LIKE(HttpStatus.CONFLICT, "CO001", "이미 좋아요한 코스입니다."),
    DUPLICATE_COURSE_BOOKMARK(HttpStatus.CONFLICT, "CO002", "이미 북마크한 코스입니다."),
    DUPLICATE_FRIEND_REQUEST(HttpStatus.CONFLICT, "CO003", "이미 친구 요청을 보냈습니다."),
    CONCURRENT_MODIFICATION_ERROR(HttpStatus.CONFLICT, "CO004", "다른 사용자가 동시에 수정하여 충돌이 발생했습니다."),
    OPTIMISTIC_LOCK_ERROR(HttpStatus.CONFLICT, "CO005", "데이터가 다른 사용자에 의해 수정되었습니다. 새로고침 후 다시 시도해주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
