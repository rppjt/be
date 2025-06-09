package runrush.be.runningrecord.controller;

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

@RestController
@RequestMapping("/running-record")
@RequiredArgsConstructor
public class RunningRecordController {
    private final RunningRecordService runningRecordService;

    @PostMapping
    public ResponseEntity<Void> createRunningRecord(@AuthenticationPrincipal UserPrincipal user,
                                                    @RequestPart("image") MultipartFile image,
                                                    @RequestPart("data") RunningRecordRequest request) {
        runningRecordService.saveRunningRecord(request, user.getId(), image);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<RunningRecordListResponse>> getRunningRecords(@AuthenticationPrincipal UserPrincipal user) {
        List<RunningRecordListResponse> runningRecords = runningRecordService.getRunningRecords(user.getId());
        return ResponseEntity.ok(runningRecords);
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<RunningRecordResponse> getRunningRecord(@PathVariable Long recordId,
                                                                  @AuthenticationPrincipal UserPrincipal user) {
        RunningRecordResponse runningRecord = runningRecordService.getRunningRecord(recordId, user.getEmail());
        return ResponseEntity.ok(runningRecord);
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<Void> deleteRunningRecord(@PathVariable Long recordId,
                                                    @AuthenticationPrincipal UserPrincipal user) {
        runningRecordService.deleteRunningRecord(recordId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/deleted")
    public ResponseEntity<List<RunningRecordListResponse>> getDeletedRecords(@AuthenticationPrincipal UserPrincipal user) {
        List<RunningRecordListResponse> deletedRecord = runningRecordService.getDeletedRecord(user.getId());
        return ResponseEntity.ok(deletedRecord);
    }

    @PutMapping("/restore/{recordId}")
    public ResponseEntity<Void> restoreRunningRecord(
            @PathVariable Long recordId,
            @AuthenticationPrincipal UserPrincipal user) {
        runningRecordService.restoreRunningRecord(recordId, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/permanent/{recordId}")
    public ResponseEntity<Void> permanentDeleteRunningRecord(
            @PathVariable Long recordId,
            @AuthenticationPrincipal UserPrincipal user) {
        runningRecordService.permanentlyDeleteRecord(recordId, user.getId());
        return ResponseEntity.noContent().build();
    }
}