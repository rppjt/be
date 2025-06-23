package runrush.be.courselike.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.courselike.service.CourseLikeService;

@Tag(name = "Course Like", description = "코스 좋아요 관리 API")
@RestController
@RequestMapping("/like")
@RequiredArgsConstructor
public class CourseLikeController {
    private final CourseLikeService courseLikeService;

    @Operation(summary = "코스 좋아요 토글", description = "코스의 좋아요 상태를 토글합니다. 좋아요가 설정되어 있으면 취소하고, 그렇지 않으면 설정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "좋아요 토글 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{courseId}")
    public ResponseEntity<Void> toggleLike(@Parameter(description = "코스 ID") @PathVariable Long courseId,
                                           @AuthenticationPrincipal UserPrincipal user) {
        courseLikeService.likeToggle(user.getId(), courseId);
        return ResponseEntity.noContent().build();
    }
}