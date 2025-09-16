package runrush.be.friends.dto;

public record FriendResponse(
        Long friendId,
        String name,
        String profileImage
) {
}