package runrush.be.friends.dto;

public record SentFriendRequestResponse(
        Long targetId,
        String name,
        String profileImage
) {
}