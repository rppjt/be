package runrush.be.coursebookmark.controller;

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

@RestController
@RequestMapping("/course/bookmark")
@RequiredArgsConstructor
public class CourseBookmarkController {
    private final CourseBookmarkService courseBookmarkService;

    @PatchMapping("/{courseId}")
    public ResponseEntity<BookmarkResponse> setBookmark(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody BookmarkRequest request) {

        BookmarkResponse response = courseBookmarkService.setBookmark(
                user.getId(),
                courseId,
                request.isBookmarked()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookmarkedCourseListResponse>> getMyBookmarkedCourses(@AuthenticationPrincipal UserPrincipal user) {
        List<BookmarkedCourseListResponse> bookmarkedCourses = courseBookmarkService.getBookmarkedCourses(user.getId());
        return ResponseEntity.ok(bookmarkedCourses);
    }

    @GetMapping("/status/{courseId}")
    public ResponseEntity<BookmarkResponse> getBookmarkStatus(
            @PathVariable Long courseId,
            @AuthenticationPrincipal UserPrincipal user) {

        BookmarkResponse response = courseBookmarkService.getBookmarkStatus(user.getId(), courseId);
        return ResponseEntity.ok(response);
    }
}