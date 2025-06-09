package runrush.be.user.dto;

public record UserUpdateRequest(
        String nickname,
        String profileImage
) {
}