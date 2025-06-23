package runrush.be.recommendedCourse.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.recommendedCourse.dto.RecommendedCourseListResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseMyListResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseResponse;
import runrush.be.recommendedCourse.dto.RecommendedCourseUpdateRequest;
import runrush.be.recommendedCourse.enums.SortType;
import runrush.be.recommendedCourse.service.RecommendedCourseService;

import java.util.List;

@Tag(name = "Recommended Course", description = "추천 코스 관리 API")
@RestController
@RequestMapping("/course")
@RequiredArgsConstructor
public class RecommendedCourseController {
    private final RecommendedCourseService recommendedCourseService;

    @Operation(summary = "추천 코스 생성", description = "러닝 기록을 기반으로 추천 코스를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "추천 코스 생성 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "러닝 기록을 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{recordId}")
    public ResponseEntity<Void> createCourse(@Parameter(description = "러닝 기록 ID") @PathVariable Long recordId,
                                             @AuthenticationPrincipal UserPrincipal user) {
        recommendedCourseService.createRecommendedCourse(recordId, user.getId(), user.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "추천 코스 수정", description = "추천 코스의 제목과 설명을 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "추천 코스 수정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "추천 코스를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{courseId}")
    public ResponseEntity<Void> updateCourse(@Parameter(description = "추천 코스 ID") @PathVariable Long courseId,
                                             @AuthenticationPrincipal UserPrincipal user,
                                             @RequestBody RecommendedCourseUpdateRequest request) {
        recommendedCourseService.updateRecommendedCourse(courseId, user.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "추천 코스 삭제", description = "추천 코스를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "추천 코스 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "권한 없음"),
            @ApiResponse(responseCode = "404", description = "추천 코스를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@Parameter(description = "추천 코스 ID") @PathVariable Long courseId,
                                             @AuthenticationPrincipal UserPrincipal user) {
        recommendedCourseService.deleteRecommendedCourse(courseId, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "추천 코스 상세 조회", description = "추천 코스의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추천 코스 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "추천 코스를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{courseId}")
    public ResponseEntity<RecommendedCourseResponse> getCourse(@Parameter(description = "추천 코스 ID") @PathVariable Long courseId,
                                                               @AuthenticationPrincipal UserPrincipal user) {
        RecommendedCourseResponse recommendedCourse = recommendedCourseService.getRecommendedCourseDetail(courseId, user.getId());
        return ResponseEntity.ok(recommendedCourse);
    }

    @Operation(summary = "내 추천 코스 목록 조회", description = "현재 사용자가 생성한 추천 코스 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "내 추천 코스 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ResponseEntity<List<RecommendedCourseMyListResponse>> getMyRecommendedCourses(@AuthenticationPrincipal UserPrincipal user) {
        List<RecommendedCourseMyListResponse> myRecommendedCourses = recommendedCourseService.getMyRecommendedCourses(user.getId());
        return ResponseEntity.ok(myRecommendedCourses);
    }

    @Operation(summary = "추천 코스 목록 조회", description = "모든 추천 코스 목록을 정렬 옵션에 따라 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추천 코스 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping
    public ResponseEntity<List<RecommendedCourseListResponse>> getAllRecommendedCourses(
            @Parameter(description = "정렬 타입 (LIKE: 좋아요순, RECENT: 최신순)") @RequestParam(defaultValue = "LIKE") SortType sortType,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<RecommendedCourseListResponse> recommendedCourses = recommendedCourseService.getRecommendedCourses(sortType, user.getId());
        return ResponseEntity.ok(recommendedCourses);
    }
}