package runrush.be.user.dto;

import runrush.be.user.domain.Level;
import runrush.be.user.domain.User;

public record UserInfoResponse(
        Long userId,
        String email,
        String name,
        String profileImage,
        String nickname,
        Level level,
        int experiencePoints
) {
    public static UserInfoResponse fromEntity(User user) {
        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImage(),
                user.getNickname(),
                user.getLevel(),
                user.getExperiencePoints()
        );
    }
}