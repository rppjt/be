package runrush.be.location.dto;

public record LocationSharingResponse(
        Long userId,
        Boolean isSharing,
        String message
) {
    public static LocationSharingResponse enabled(Long userId) {
        return new LocationSharingResponse(
                userId,
                true,
                "위치 공유가 활성화되었습니다. 친구들이 회원님의 위치를 볼 수 있습니다."
        );
    }

    public static LocationSharingResponse disabled(Long userId) {
        return new LocationSharingResponse(
                userId,
                false,
                "위치 공유가 비활성화되었습니다. 친구들이 회원님의 위치를 볼 수 없습니다."
        );
    }

    public static LocationSharingResponse current(Long userId, Boolean isSharing) {
        return new LocationSharingResponse(userId, isSharing, null);
    }
}