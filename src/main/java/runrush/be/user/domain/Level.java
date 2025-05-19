package runrush.be.user.domain;

public enum Level {
    BEGINNER(1, "초보 러너", 0, 100),
    INTERMEDIATE(2, "열정 러너", 100, 300),
    ADVANCED(3, "숙련 러너", 300, 600),
    EXPERT(4, "엘리트 러너", 600, 1000);

    private final int value;
    private final String title;
    private final int minExp;
    private final int maxExp;

    Level(int value, String title, int minExp, int maxExp) {
        this.value = value;
        this.title = title;
        this.minExp = minExp;
        this.maxExp = maxExp;
    }
}