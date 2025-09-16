package runrush.be.coursebookmark.dto;

public record BookmarkResponse(
        Long courseId,
        boolean isBookmarked,
        String message
) {
    public static BookmarkResponse bookmarked(Long courseId) {
        return new BookmarkResponse(courseId, true, "코스가 즐겨찾기에 추가되었습니다.");
    }

    public static BookmarkResponse unbookmarked(Long courseId) {
        return new BookmarkResponse(courseId, false, "코스가 즐겨찾기에서 제거되었습니다.");
    }

    public static BookmarkResponse current(Long courseId, Boolean isBookmarked) {
        return new BookmarkResponse(courseId, isBookmarked, null);
    }
}