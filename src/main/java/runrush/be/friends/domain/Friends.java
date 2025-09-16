package runrush.be.friends.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import runrush.be.user.domain.User;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "friends", 
       indexes = {
           @Index(name = "idx_friends_requester_status", columnList = "requester_id, friend_status"),
           @Index(name = "idx_friends_target_status", columnList = "target_id, friend_status")
       },
       uniqueConstraints = {
           @UniqueConstraint(
               name = "uk_friends_direction", 
               columnNames = {"requester_id", "target_id"}
           )
       })
public class Friends {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id")
    private User target;

    @Enumerated(EnumType.STRING)
    @Column(name = "friend_status")
    private FriendStatus status;

    @Version
    private Long version;

    @Builder
    public Friends(User requester, User target) {
        this.requester = requester;
        this.target = target;
        this.status = FriendStatus.PENDING;
    }

    public void accept() {
        this.status = FriendStatus.ACCEPTED;
    }
}