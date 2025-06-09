package runrush.be.friends.dto;

public record ReceivedFriendRequestResponse(
        Long requesterId,
        String name,
        String profileImage
) {
}