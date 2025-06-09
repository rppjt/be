package runrush.be.user.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_id")
    private String kakaoId;

    private String email;

    private String name;

    @Column(name = "profile_image")
    private String profileImage;

    @Column(unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Level level;

    private int experiencePoints;

    @Builder
    public User(String kakaoId, String email, String name, String profileImage, String nickname) {
        this.kakaoId = kakaoId;
        this.email = email;
        this.name = name;
        this.profileImage = profileImage;
        this.nickname = nickname;
        this.level = Level.BEGINNER;
        this.experiencePoints = 0;
    }

    public void addExperiencePoints(int points) {
        this.experiencePoints += points;
        this.level = Level.fromExperiencePoints(experiencePoints);
    }

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }
}