package runrush.be.runningrecord.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.runningrecord.dto.RunningRecordListResponse;
import runrush.be.runningrecord.dto.RunningRecordRequest;
import runrush.be.runningrecord.dto.RunningRecordResponse;
import runrush.be.runningrecord.service.RunningRecordService;

import java.util.List;

@Tag(name = "Running Record", description = "러닝 기록 관리 API")
@RestController
@RequestMapping("/running-record")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class RunningRecordController {
    private final RunningRecordService runningRecordService;

    @Operation(summary = "러닝 기록 생성", description = "새로운 러닝 기록을 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "러닝 기록 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<Void> createRunningRecord(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user,
            @Parameter(description = "러닝 기록 이미지") @RequestPart("image") MultipartFile image,
            @Parameter(description = "러닝 기록 데이터") @RequestPart("data") RunningRecordRequest request) {
        runningRecordService.saveRunningRecord(request, user.getId(), image);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "러닝 기록 목록 조회", description = "사용자의 러닝 기록 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "러닝 기록 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = RunningRecordListResponse.class)))
    @GetMapping
    public ResponseEntity<List<RunningRecordListResponse>> getRunningRecords(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user) {
        List<RunningRecordListResponse> runningRecords = runningRecordService.getRunningRecords(user.getId());
        return ResponseEntity.ok(runningRecords);
    }

    @Operation(summary = "러닝 기록 상세 조회", description = "특정 러닝 기록의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "러닝 기록 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "러닝 기록을 찾을 수 없음")
    })
    @GetMapping("/{recordId}")
    public ResponseEntity<RunningRecordResponse> getRunningRecord(@Parameter(description = "러닝 기록 ID") @PathVariable Long recordId,
                                                                  @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user) {
        RunningRecordResponse runningRecord = runningRecordService.getRunningRecord(recordId, user.getEmail());
        return ResponseEntity.ok(runningRecord);
    }

    @Operation(summary = "러닝 기록 삭제", description = "러닝 기록을 소프트 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "러닝 기록 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "러닝 기록을 찾을 수 없음")
    })
    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteRunningRecord(@Parameter(description = "러닝 기록 ID") @PathVariable Long recordId,
                                                    @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user) {
        runningRecordService.deleteRunningRecord(recordId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "삭제된 러닝 기록 목록 조회", description = "소프트 삭제된 러닝 기록 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제된 러닝 기록 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/deleted")
    public ResponseEntity<List<RunningRecordListResponse>> getDeletedRecords(@Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user) {
        List<RunningRecordListResponse> deletedRecord = runningRecordService.getDeletedRecord(user.getId());
        return ResponseEntity.ok(deletedRecord);
    }

    @Operation(summary = "러닝 기록 복구", description = "소프트 삭제된 러닝 기록을 복구합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "러닝 기록 복구 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "러닝 기록을 찾을 수 없음")
    })
    @PutMapping("/restore/{recordId}")
    public ResponseEntity<Void> restoreRunningRecord(
            @Parameter(description = "복구할 러닝 기록 ID") @PathVariable Long recordId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user) {
        runningRecordService.restoreRunningRecord(recordId, user.getId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "러닝 기록 영구 삭제", description = "러닝 기록을 영구적으로 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "러닝 기록 영구 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "러닝 기록을 찾을 수 없음")
    })
    @DeleteMapping("/permanent/{recordId}")
    public ResponseEntity<Void> permanentDeleteRunningRecord(
            @Parameter(description = "영구 삭제할 러닝 기록 ID") @PathVariable Long recordId,
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal user) {
        runningRecordService.permanentlyDeleteRecord(recordId, user.getId());
        return ResponseEntity.noContent().build();
    }
}