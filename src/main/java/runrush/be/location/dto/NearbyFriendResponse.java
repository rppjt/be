package runrush.be.location.dto;

public record NearbyFriendResponse(
        Long userId,
        String nickname,
        String profileImage,
        Double latitude,
        Double longitude,
        Double distance
) {
}