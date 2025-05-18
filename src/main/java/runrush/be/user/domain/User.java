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

    @Enumerated(EnumType.STRING)
    private Level level;

    private int experiencePoints;

    @Builder
    public User(String kakaoId, String email, String name, String profileImage) {
        this.kakaoId = kakaoId;
        this.email = email;
        this.name = name;
        this.profileImage = profileImage;
        this.level = Level.BEGINNER;
        this.experiencePoints = 0;
    }
}