package runrush.be.concurrency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import runrush.be.courselike.repository.CourseLikeRepository;
import runrush.be.courselike.service.CourseLikeService;
import runrush.be.coursebookmark.repository.CourseBookmarkRepository;
import runrush.be.coursebookmark.service.CourseBookmarkService;
import runrush.be.friends.repository.FriendsRepository;
import runrush.be.friends.service.FriendsService;
import runrush.be.recommendedCourse.domain.RecommendedCourse;
import runrush.be.recommendedCourse.repository.RecommendedCourseRepository;
import runrush.be.user.domain.User;
import runrush.be.user.repository.UserRepository;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("간단한 동시성 제어 검증 테스트")
public class ConcurrencyTest {

    @Autowired
    private CourseLikeService courseLikeService;
    
    @Autowired
    private CourseBookmarkService courseBookmarkService;
    
    @Autowired
    private FriendsService friendsService;
    
    @Autowired
    private CourseLikeRepository courseLikeRepository;
    
    @Autowired
    private CourseBookmarkRepository courseBookmarkRepository;
    
    @Autowired
    private FriendsRepository friendsRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RecommendedCourseRepository recommendedCourseRepository;

    private User testUser1;
    private User testUser2;
    private RecommendedCourse testCourse;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리
        courseLikeRepository.deleteAll();
        courseBookmarkRepository.deleteAll();
        friendsRepository.deleteAll();
        recommendedCourseRepository.deleteAll();
        userRepository.deleteAll();
        
        // 테스트 사용자들 생성
        testUser1 = User.builder()
                .kakaoId("concurrency_test1")
                .email("concurrency1@example.com")
                .name("동시성테스터1")
                .nickname("concurrent_tester1")
                .profileImage("image1.jpg")
                .build();
        
        testUser2 = User.builder()
                .kakaoId("concurrency_test2")
                .email("concurrency2@example.com")
                .name("동시성테스터2")
                .nickname("concurrent_tester2")
                .profileImage("image2.jpg")
                .build();
        
        userRepository.save(testUser1);
        userRepository.save(testUser2);
        
        // 테스트 추천 코스 생성
        testCourse = RecommendedCourse.builder()
                .user(testUser1)
                .title("동시성 테스트 코스")
                .description("동시성 테스트용")
                .totalDistance(5000.0)
                .pathGeoJson("{\"type\":\"LineString\",\"coordinates\":[[127.0,37.0],[127.1,37.1]]}")
                .build();
        
        recommendedCourseRepository.save(testCourse);
    }

    @Test
    @DisplayName("좋아요 토글 동시성 테스트")
    void testLikeToggleConcurrency() throws InterruptedException {
        final int threadCount = 50;
        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);

        System.out.println("=== 좋아요 토글 동시성 테스트 시작 ===");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    courseLikeService.likeToggle(testUser2.getId(), testCourse.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.out.println("좋아요 토글 에러: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        long likeCount = courseLikeRepository.countByRecommendedCourseId(testCourse.getId());

        System.out.println("실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("총 스레드: " + threadCount);
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("에러 요청: " + errorCount.get());
        System.out.println("최종 좋아요 개수: " + likeCount);

        // 동시성 제어 검증
        assertThat(likeCount).isIn(0L, 1L); // 0개 또는 1개만 허용
        assertThat(successCount.get() + errorCount.get()).isEqualTo(threadCount);
        
        System.out.println("좋아요 동시성 제어 성공!");
    }

    @Test
    @DisplayName("북마크 동시성 테스트")
    void testBookmarkConcurrency() throws InterruptedException {
        final int threadCount = 30;
        final ExecutorService executorService = Executors.newFixedThreadPool(5);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);

        System.out.println("=== 북마크 동시성 테스트 시작 ===");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    courseBookmarkService.setBookmark(testUser2.getId(), testCourse.getId(), true);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("북마크 설정 에러: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        boolean bookmarkExists = courseBookmarkRepository.existsByUserIdAndRecommendedCourseId(
                testUser2.getId(), testCourse.getId()
        );
        long bookmarkCount = courseBookmarkRepository.findWithCourseByUserId(testUser2.getId()).size();

        System.out.println("실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("총 스레드: " + threadCount);
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("북마크 존재: " + bookmarkExists);
        System.out.println("총 북마크 개수: " + bookmarkCount);

        // 동시성 제어 검증
        assertThat(bookmarkExists).isTrue();
        assertThat(bookmarkCount).isEqualTo(1L);
        
        System.out.println("✅ 북마크 동시성 제어 성공!");
    }

    @Test
    @DisplayName("친구 요청 동시성 테스트")
    void testFriendRequestConcurrency() throws InterruptedException {
        final int threadCount = 20;
        final ExecutorService executorService = Executors.newFixedThreadPool(5);
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger errorCount = new AtomicInteger(0);

        System.out.println("=== 친구 요청 동시성 테스트 시작 ===");
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    friendsService.sendFriendRequest(testUser1.getId(), testUser2.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.out.println("친구 요청 에러: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();
        boolean relationExists = friendsRepository.findByRequesterIdAndTargetId(
                testUser1.getId(), testUser2.getId()
        ).isPresent();

        System.out.println("실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("총 스레드: " + threadCount);
        System.out.println("성공 요청: " + successCount.get());
        System.out.println("에러 요청: " + errorCount.get());
        System.out.println("친구 요청 존재: " + relationExists);

        // 동시성 제어 검증
        assertThat(relationExists).isTrue();
        assertThat(successCount.get()).isGreaterThan(0);
        
        System.out.println("친구 요청 동시성 제어 성공!");
    }

    @Test
    @DisplayName("종합 동시성 테스트")
    void testCombinedConcurrency() throws InterruptedException {
        final int operationsPerType = 10;
        final int totalOperations = operationsPerType * 3; // 좋아요, 북마크, 친구요청
        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        final CountDownLatch latch = new CountDownLatch(totalOperations);
        
        final AtomicInteger likeSuccess = new AtomicInteger(0);
        final AtomicInteger bookmarkSuccess = new AtomicInteger(0);
        final AtomicInteger friendSuccess = new AtomicInteger(0);

        System.out.println("=== 종합 동시성 테스트 시작 ===");
        long startTime = System.currentTimeMillis();

        // 좋아요 토글 (여러 번 토글되므로 최종 상태는 예측 불가)
        for (int i = 0; i < operationsPerType; i++) {
            executorService.submit(() -> {
                try {
                    courseLikeService.likeToggle(testUser2.getId(), testCourse.getId());
                    likeSuccess.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("종합테스트 좋아요 에러: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 북마크 설정 (모두 추가 요청)
        for (int i = 0; i < operationsPerType; i++) {
            executorService.submit(() -> {
                try {
                    courseBookmarkService.setBookmark(testUser2.getId(), testCourse.getId(), true);
                    bookmarkSuccess.incrementAndGet();
                } catch (Exception e) {
                    System.out.println("종합테스트 북마크 에러: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        // 친구 요청 (중복 요청, 대부분 에러 예상)
        for (int i = 0; i < operationsPerType; i++) {
            executorService.submit(() -> {
                try {
                    friendsService.sendFriendRequest(testUser1.getId(), testUser2.getId());
                    friendSuccess.incrementAndGet();
                } catch (Exception e) {
                    // 중복 요청 에러는 정상적인 동작
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();
        
        long endTime = System.currentTimeMillis();

        // 최종 상태 확인
        long finalLikeCount = courseLikeRepository.countByRecommendedCourseId(testCourse.getId());
        boolean hasFinalBookmark = courseBookmarkRepository.existsByUserIdAndRecommendedCourseId(
                testUser2.getId(), testCourse.getId()
        );
        boolean hasFriendRequest = friendsRepository.findByRequesterIdAndTargetId(
                testUser1.getId(), testUser2.getId()
        ).isPresent();

        System.out.println("=== 종합 동시성 테스트 결과 ===");
        System.out.println("실행 시간: " + (endTime - startTime) + "ms");
        System.out.println("좋아요 성공: " + likeSuccess.get() + "/" + operationsPerType);
        System.out.println("북마크 성공: " + bookmarkSuccess.get() + "/" + operationsPerType);
        System.out.println("친구요청 성공: " + friendSuccess.get() + "/" + operationsPerType);
        System.out.println("최종 좋아요 개수: " + finalLikeCount + " (0 또는 1이어야 함)");
        System.out.println("최종 북마크 존재: " + hasFinalBookmark + " (true여야 함)");
        System.out.println("친구요청 존재: " + hasFriendRequest + " (true여야 함)");

        // 데이터 일관성 검증
        assertThat(finalLikeCount).isIn(0L, 1L);
        assertThat(hasFinalBookmark).isTrue();
        assertThat(hasFriendRequest).isTrue();
        assertThat(friendSuccess.get()).isEqualTo(1); // 친구 요청은 정확히 1번만 성공

        System.out.println("모든 동시성 제어가 정상 동작합니다!");
    }
}