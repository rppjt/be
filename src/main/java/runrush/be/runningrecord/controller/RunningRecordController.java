package runrush.be.runningrecord.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
                                                    @RequestBody RunningRecordRequest request) {
        runningRecordService.saveRunningRecord(request, user.getEmail());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<RunningRecordListResponse>> getRunningRecords(@AuthenticationPrincipal UserPrincipal user) {
        List<RunningRecordListResponse> runningRecords = runningRecordService.getRunningRecords(user.getEmail());
        return ResponseEntity.ok(runningRecords);
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<RunningRecordResponse> getRunningRecord(@PathVariable Long recordId) {
        RunningRecordResponse runningRecord = runningRecordService.getRunningRecord(recordId);
        return ResponseEntity.ok(runningRecord);
    }
}