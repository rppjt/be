package runrush.be.coursebookmark.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.coursebookmark.dto.BookmarkToggleResponse;
import runrush.be.coursebookmark.dto.BookmarkedCourseListResponse;
import runrush.be.coursebookmark.service.CourseBookmarkService;

import java.util.List;

@RestController
@RequestMapping("/course/bookmark")
@RequiredArgsConstructor
public class CourseBookmarkController {
    private final CourseBookmarkService courseBookmarkService;

    @PostMapping("/{courseId}")
    public ResponseEntity<BookmarkToggleResponse> toggleBookmark(@PathVariable Long courseId,
                                                                 @AuthenticationPrincipal UserPrincipal user) {
        BookmarkToggleResponse bookmarkToggleResponse = courseBookmarkService.toggleBookmark(user.getId(), courseId);
        return ResponseEntity.ok(bookmarkToggleResponse);
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookmarkedCourseListResponse>> getMyBookmarkedCourses(@AuthenticationPrincipal UserPrincipal user) {
        List<BookmarkedCourseListResponse> bookmarkedCourses = courseBookmarkService.getBookmarkedCourses(user.getId());
        return ResponseEntity.ok(bookmarkedCourses);
    }
}