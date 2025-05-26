package runrush.be.coursebookmark.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.coursebookmark.dto.BookmarkToggleResponse;
import runrush.be.coursebookmark.service.CourseBookmarkService;

@RestController
@RequestMapping("/course/bookmark")
@RequiredArgsConstructor
public class CourseBookmarkController {
    private final CourseBookmarkService courseBookmarkService;

    @PostMapping("/{courseId}")
    public ResponseEntity<BookmarkToggleResponse> toggleBookmark(@PathVariable Long courseId,
                                                                 @AuthenticationPrincipal UserPrincipal user) {
        BookmarkToggleResponse bookmarkToggleResponse = courseBookmarkService.toggleBookmark(user.getId(), courseId);
        return ResponseEntity.ok().body(bookmarkToggleResponse);
    }
}