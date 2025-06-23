package runrush.be.coursebookmark.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.coursebookmark.dto.BookmarkRequest;
import runrush.be.coursebookmark.dto.BookmarkResponse;
import runrush.be.coursebookmark.dto.BookmarkedCourseListResponse;
import runrush.be.coursebookmark.service.CourseBookmarkService;

import java.util.List;

@Tag(name = "Course Bookmark", description = "코스 북마크 관리 API")
@RestController
@RequestMapping("/course/bookmark")
@RequiredArgsConstructor
public class CourseBookmarkController {
    private final CourseBookmarkService courseBookmarkService;

    @Operation(summary = "코스 북마크 설정", description = "코스의 북마크 상태를 설정하거나 해제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 설정 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{courseId}")
    public ResponseEntity<BookmarkResponse> setBookmark(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody BookmarkRequest request) {

        BookmarkResponse response = courseBookmarkService.setBookmark(
                user.getId(),
                courseId,
                request.isBookmarked()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "내 북마크 코스 목록 조회", description = "사용자가 북마크한 코스 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 코스 목록 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ResponseEntity<List<BookmarkedCourseListResponse>> getMyBookmarkedCourses(@AuthenticationPrincipal UserPrincipal user) {
        List<BookmarkedCourseListResponse> bookmarkedCourses = courseBookmarkService.getBookmarkedCourses(user.getId());
        return ResponseEntity.ok(bookmarkedCourses);
    }

    @Operation(summary = "코스 북마크 상태 조회", description = "특정 코스의 북마크 상태를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "북마크 상태 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/status/{courseId}")
    public ResponseEntity<BookmarkResponse> getBookmarkStatus(
            @Parameter(description = "코스 ID") @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal user) {

        BookmarkResponse response = courseBookmarkService.getBookmarkStatus(user.getId(), courseId);
        return ResponseEntity.ok(response);
    }
}