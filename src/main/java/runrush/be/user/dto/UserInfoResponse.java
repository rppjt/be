package runrush.be.user.dto;

import runrush.be.user.domain.Level;
import runrush.be.user.domain.User;

public record UserInfoResponse(
        String email,
        String name,
        String profileImage,
        Level level,
        int experiencePoints
) {
    public static UserInfoResponse fromEntity(User user) {
        return new UserInfoResponse(
                user.getEmail(),
                user.getName(),
                user.getProfileImage(),
                user.getLevel(),
                user.getExperiencePoints()
        );
    }
}