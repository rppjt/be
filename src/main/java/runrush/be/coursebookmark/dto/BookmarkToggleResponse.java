package runrush.be.coursebookmark.dto;

public record BookmarkToggleResponse(
        boolean isBookmarked,
        String message
) {
    public static BookmarkToggleResponse bookmarked() {
        return new BookmarkToggleResponse(true, "코스가 즐겨찾기에 추가되었습니다.");
    }

    public static BookmarkToggleResponse unbookmarked() {
        return new BookmarkToggleResponse(false, "코스가 즐겨찾기에서 제거되었습니다.");
    }
}