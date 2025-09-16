package runrush.be.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import runrush.be.user.domain.Level;
import runrush.be.user.domain.User;
import runrush.be.user.repository.UserRepository;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 성능 테스트용 더미 데이터 생성기
 * 
 * 실행 조건:
 * - runrush.dummy-data.enabled=true (application.yml에 설정됨)
 * - 기존 데이터가 없을 때만 실행
 * 
 * 비활성화 방법:
 * application.yml에서 runrush.dummy-data.enabled=false로 변경
 * 
 * 생성 데이터:
 * - Users: 1,000명
 * - RunningRecords: 1,000,000건 (100만건) - 성능 최적화 효과 측정용
 * - 기타 엔티티들 (예정)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "runrush.dummy-data.enabled", 
    havingValue = "true", 
    matchIfMissing = false
)
public class PerformanceTestDataLoader implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random(12345); // 시드값 고정으로 일관된 데이터
    
    @Override
    public void run(String... args) throws Exception {
        // 기존 데이터가 있으면 건너뛰기
        if (userRepository.count() > 0) {
            log.info("⏭️ 기존 사용자 데이터가 {}건 있어 더미 데이터 생성을 건너뜁니다.", 
                userRepository.count());
            return;
        }
        
        log.info("🚀 빈 데이터베이스 감지! 성능 테스트용 더미 데이터 생성을 시작합니다.");
        long startTime = System.currentTimeMillis();
        
        // 1단계: 사용자 데이터 생성
        generateUsers(1000);
        
        // 2단계: 러닝 기록 대량 생성 (배치 처리) - 100만건으로 성능 최적화 효과 극대화
        generateRunningRecords(1000000);
        
        // 3단계: 추천 코스 생성 (배치 처리)
        generateRecommendedCourses(8000);
        
        // 4단계: 친구 관계 생성
        generateFriendsAndInteractions();
        
        // 5단계: 소셜 상호작용 생성 (북마크, 좋아요)
        generateSocialInteractions();
        
        // 6단계: 추천 코스를 따라 뛴 러닝 기록들 생성 (성능 테스트용)
        generateCourseFollowingRecords();
        
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        
        log.info("✅ 더미 데이터 생성 완료! 총 소요 시간: {:.2f}초", duration);
        printDataSummary();
    }
    
    /**
     * 사용자 1,000명 생성
     */
    @Transactional
    protected void generateUsers(int count) {
        log.info("👥 사용자 {} 명 생성 중...", count);
        
        List<User> users = new ArrayList<>();
        
        for (int i = 1; i <= count; i++) {
            User user = User.builder()
                .kakaoId("kakao_test_" + String.format("%06d", i))
                .email("testuser" + i + "@runrush-demo.com")
                .name("테스트사용자" + i)
                .profileImage("https://via.placeholder.com/150?text=User" + i)
                .nickname("runner" + i)
                .build();
            
            users.add(user);
            
            // 100명씩 배치로 저장 (메모리 효율성)
            if (i % 100 == 0) {
                userRepository.saveAll(users);
                users.clear();
                log.debug("사용자 {} 명 저장 완료", i);
            }
        }
        
        // 나머지 사용자들 저장
        if (!users.isEmpty()) {
            userRepository.saveAll(users);
        }
        
        log.info("✅ 사용자 생성 완료: {} 명", count);
    }
    
    /**
     * 러닝 기록 100만건 생성 - JdbcTemplate 배치 처리로 성능 최적화
     * 인덱스 최적화 전후 비교를 위한 대용량 데이터
     */
    @Transactional
    protected void generateRunningRecords(int count) {
        log.info("🏃 러닝 기록 {} 건 생성 중... (배치 처리로 최적화)", count);
        
        String sql = """
            INSERT INTO running_record 
            (user_id, started_time, ended_time, total_distance, total_time, pace,
             start_latitude, start_longitude, end_latitude, end_longitude,
             start_location_name, end_location_name, path_geo_json, 
             is_deleted, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?)
            """;
        
        long startTime = System.currentTimeMillis();
        
        // 2만건씩 배치로 처리 (100만건에 최적화된 배치 크기)
        int batchSize = 20000;
        int totalBatches = (count + batchSize - 1) / batchSize;
        
        for (int batch = 0; batch < totalBatches; batch++) {
            final int batchStart = batch * batchSize;
            final int batchEnd = Math.min((batch + 1) * batchSize, count);
            final int currentBatchSize = batchEnd - batchStart;
            
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    int recordIndex = batchStart + i;
                    
                    // 사용자 ID (1~1000 순환)
                    long userId = (recordIndex % 1000) + 1;
                    
                    // 현실적인 러닝 시간 생성 (최근 1년간)
                    LocalDateTime startTime = generateRealisticRunningTime(recordIndex);
                    
                    // 사용자 레벨에 따른 현실적인 거리와 페이스
                    Level userLevel = getUserLevel(userId);
                    double distance = generateRealisticDistance(userLevel);
                    double pace = generateRealisticPace(userLevel, distance);
                    long totalTime = (long) (distance / 1000.0 * pace * 60); // 초 단위
                    
                    LocalDateTime endTime = startTime.plusSeconds(totalTime);
                    
                    // 서울 지역 GPS 좌표
                    double[] startCoords = generateSeoulCoordinates();
                    double[] endCoords = generateNearbyCoordinates(startCoords, distance);
                    
                    String startLocation = generateLocationName();
                    String endLocation = distance > 3000 ? generateLocationName() : startLocation;
                    
                    // 간단한 GeoJSON 생성
                    String geoJson = generateSimpleGeoJson(startCoords, endCoords);
                    
                    ps.setLong(1, userId);
                    ps.setTimestamp(2, Timestamp.valueOf(startTime));
                    ps.setTimestamp(3, Timestamp.valueOf(endTime));
                    ps.setDouble(4, distance);
                    ps.setLong(5, totalTime);
                    ps.setDouble(6, pace);
                    ps.setDouble(7, startCoords[0]); // latitude
                    ps.setDouble(8, startCoords[1]); // longitude
                    ps.setDouble(9, endCoords[0]);
                    ps.setDouble(10, endCoords[1]);
                    ps.setString(11, startLocation);
                    ps.setString(12, endLocation);
                    ps.setString(13, geoJson);
                    ps.setTimestamp(14, Timestamp.valueOf(startTime));
                }
                
                @Override
                public int getBatchSize() {
                    return currentBatchSize;
                }
            });
            
            // 진행률 로깅 (100만건이므로 5배치마다)
            if ((batch + 1) % 5 == 0 || batch == totalBatches - 1) {
                int processedRecords = Math.min((batch + 1) * batchSize, count);
                double progress = (double) processedRecords / count * 100;
                long currentTime = System.currentTimeMillis();
                double elapsedSeconds = (currentTime - startTime) / 1000.0;
                log.info("진행률: {}/{} 배치 완료 ({:.1f}% - {} / {} 건, {:.1f}초 소요)", 
                    batch + 1, totalBatches, progress, processedRecords, count, elapsedSeconds);
            }
        }
        
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        log.info("✅ 러닝 기록 생성 완료: {} 건 ({:.2f}초 소요)", count, duration);
    }
    
    // === 현실적인 데이터 생성을 위한 유틸리티 메서드들 ===
    
    private LocalDateTime generateRealisticRunningTime(int index) {
        // 최근 1년간의 랜덤한 날짜
        LocalDateTime baseTime = LocalDateTime.now().minusDays(random.nextInt(365));
        
        // 평일 vs 주말 (주말 30% 확률)
        boolean isWeekend = random.nextInt(10) < 3;
        
        if (isWeekend) {
            // 주말: 오전 8-11시 또는 오후 2-6시
            int hour = random.nextBoolean() ? 8 + random.nextInt(4) : 14 + random.nextInt(5);
            return baseTime.withHour(hour).withMinute(random.nextInt(60)).withSecond(0);
        } else {
            // 평일: 저녁 6-9시 또는 새벽 6-7시 (저녁 80% 확률)
            int hour = random.nextInt(10) < 8 ? 18 + random.nextInt(4) : 6 + random.nextInt(2);
            return baseTime.withHour(hour).withMinute(random.nextInt(60)).withSecond(0);
        }
    }
    
    private Level getUserLevel(long userId) {
        // 간단한 해시 함수로 일관된 레벨 결정
        int hash = (int) (userId * 31) % 100;
        if (hash < 50) return Level.BEGINNER;      // 50%
        if (hash < 75) return Level.INTERMEDIATE;  // 25%
        if (hash < 90) return Level.ADVANCED;      // 15%
        if (hash < 97) return Level.EXPERT;        // 7%
        return Level.MASTER;                       // 3%
    }
    
    private double generateRealisticDistance(Level level) {
        return switch (level) {
            case BEGINNER -> 1000 + random.nextDouble() * 4000;      // 1-5km
            case INTERMEDIATE -> 3000 + random.nextDouble() * 7000;   // 3-10km  
            case ADVANCED -> 5000 + random.nextDouble() * 15000;     // 5-20km
            case EXPERT -> 8000 + random.nextDouble() * 17000;       // 8-25km
            case MASTER -> 10000 + random.nextDouble() * 32000;      // 10-42km (마라톤)
        };
    }
    
    private double generateRealisticPace(Level level, double distance) {
        // 기본 페이스 (분/km)
        double basePace = switch (level) {
            case BEGINNER -> 6.0 + random.nextDouble() * 2.0;    // 6-8분/km
            case INTERMEDIATE -> 5.0 + random.nextDouble() * 1.5; // 5-6.5분/km
            case ADVANCED -> 4.0 + random.nextDouble() * 1.5;    // 4-5.5분/km
            case EXPERT -> 3.5 + random.nextDouble() * 1.0;      // 3.5-4.5분/km
            case MASTER -> 3.0 + random.nextDouble() * 1.0;      // 3-4분/km (엘리트)
        };
        
        // 거리가 길수록 페이스가 느려짐
        if (distance > 10000) basePace += 0.5;
        if (distance > 15000) basePace += 0.5;
        
        return basePace;
    }
    
    private double[] generateSeoulCoordinates() {
        // 서울시 중심부 좌표 범위 (강남, 강북, 한강공원 등)
        double lat = 37.4500 + random.nextDouble() * 0.1500; // 37.45 ~ 37.60
        double lng = 126.9000 + random.nextDouble() * 0.2000; // 126.90 ~ 127.10
        return new double[]{lat, lng};
    }
    
    private double[] generateNearbyCoordinates(double[] start, double distance) {
        // 거리(미터)를 위도/경도 변화량으로 변환
        double latChange = (distance / 111000.0) * (random.nextDouble() - 0.5) * 2;
        double lngChange = (distance / 88000.0) * (random.nextDouble() - 0.5) * 2;
        
        return new double[]{
            start[0] + latChange,
            start[1] + lngChange
        };
    }
    
    private String generateLocationName() {
        String[] locations = {
            "한강공원", "올림픽공원", "남산공원", "선유도공원", "여의도공원", 
            "반포한강공원", "뚝섬공원", "망원한강공원", "청계천", "상암DMC",
            "송파나루공원", "잠실종합운동장", "용산가족공원", "효창공원", "탑골공원"
        };
        return locations[random.nextInt(locations.length)];
    }
    
    private String generateSimpleGeoJson(double[] start, double[] end) {
        // 시작점과 끝점을 연결하는 간단한 LineString
        return String.format("""
            {"type":"LineString","coordinates":[[%.6f,%.6f],[%.6f,%.6f]]}
            """, start[1], start[0], end[1], end[0]);
    }
    
    /**
     * 추천 코스 8,000건 생성 - 베스트 러닝 기록에서 추천 코스 생성
     */
    @Transactional
    protected void generateRecommendedCourses(int count) {
        log.info("🎯 추천 코스 {} 건 생성 중... (우수한 러닝 기록 기반)", count);
        
        String sql = """
            INSERT INTO recommended_course 
            (user_id, source_record_id, image_url, start_location_name, end_location_name,
             title, description, path_geo_json, total_distance, is_deleted, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?)
            """;
        
        long startTime = System.currentTimeMillis();
        
        // 2000건씩 배치로 처리
        int batchSize = 2000;
        int totalBatches = (count + batchSize - 1) / batchSize;
        
        for (int batch = 0; batch < totalBatches; batch++) {
            final int batchStart = batch * batchSize;
            final int batchEnd = Math.min((batch + 1) * batchSize, count);
            final int currentBatchSize = batchEnd - batchStart;
            
            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    int courseIndex = batchStart + i;
                    
                    // 우수한 러닝 기록을 기반으로 추천 코스 생성
                    // 사용자 ID (추천 코스는 고급 러너들이 주로 생성)
                    long userId = (courseIndex % 500) + 1; // 상위 500명 중에서
                    Level userLevel = getUserLevel(userId);
                    
                    // 고급 러너만 추천 코스 생성 (현실적)
                    if (userLevel == Level.BEGINNER) {
                        userId = (courseIndex % 100) + 801; // 고급 러너로 변경
                        userLevel = getUserLevel(userId);
                    }
                    
                    // 소스 러닝 기록 ID (실제 러닝 기록 중에서 랜덤 선택)
                    long sourceRecordId = 1 + random.nextInt(1000000); // 1~1000000 범위
                    
                    // 현실적인 추천 코스 거리 생성
                    double distance = generateRecommendedCourseDistance(userLevel);
                    
                    // 매력적인 코스 제목 생성
                    String title = generateCourseTitle(distance, courseIndex);
                    
                    // 상세한 코스 설명 생성
                    String description = generateCourseDescription(distance, userLevel);
                    
                    // 코스 생성 시간 (최근 6개월)
                    LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(180));
                    
                    // 서울 지역 GPS 좌표
                    double[] startCoords = generateSeoulCoordinates();
                    double[] endCoords = generateNearbyCoordinates(startCoords, distance);
                    
                    String startLocation = generateLocationName();
                    String endLocation = distance > 5000 ? generateLocationName() : startLocation;
                    
                    // 추천 코스용 GeoJSON (더 정교한 경로)
                    String geoJson = generateRecommendedCourseGeoJson(startCoords, endCoords, distance);
                    
                    // 이미지 URL (실제 러닝 코스 이미지 스타일)
                    String imageUrl = generateCourseImageUrl(courseIndex);
                    
                    ps.setLong(1, userId);
                    ps.setLong(2, sourceRecordId);
                    ps.setString(3, imageUrl);
                    ps.setString(4, startLocation);
                    ps.setString(5, endLocation);
                    ps.setString(6, title);
                    ps.setString(7, description);
                    ps.setString(8, geoJson);
                    ps.setDouble(9, distance);
                    ps.setTimestamp(10, Timestamp.valueOf(createdAt));
                }
                
                @Override
                public int getBatchSize() {
                    return currentBatchSize;
                }
            });
            
            // 진행률 로깅
            if ((batch + 1) % 2 == 0 || batch == totalBatches - 1) {
                int processedCourses = Math.min((batch + 1) * batchSize, count);
                double progress = (double) processedCourses / count * 100;
                long currentTime = System.currentTimeMillis();
                double elapsedSeconds = (currentTime - startTime) / 1000.0;
                log.info("진행률: {}/{} 배치 완료 ({:.1f}% - {} / {} 건, {:.1f}초 소요)", 
                    batch + 1, totalBatches, progress, processedCourses, count, elapsedSeconds);
            }
        }
        
        long endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000.0;
        log.info("✅ 추천 코스 생성 완료: {} 건 ({:.2f}초 소요)", count, duration);
    }
    
    /**
     * 친구 관계 및 소셜 상호작용 생성
     */
    @Transactional
    protected void generateFriendsAndInteractions() {
        log.info("👥 친구 관계 생성 중...");
        
        // 현실적인 친구 관계 생성 (사용자당 평균 5-15명)
        String friendSql = """
            INSERT INTO friends (requester_id, target_id, friend_status)
            VALUES (?, ?, ?)
            """;
        
        List<Object[]> friendData = new ArrayList<>();
        
        // 1000명 사용자에 대해 친구 관계 생성
        for (int userId = 1; userId <= 1000; userId++) {
            int friendCount = 5 + random.nextInt(11); // 5-15명
            
            // 중복 방지를 위한 Set
            Set<Integer> friendIds = new HashSet<>();
            
            while (friendIds.size() < friendCount) {
                int friendId = 1 + random.nextInt(1000);
                if (friendId != userId) {
                    friendIds.add(friendId);
                }
            }
            
            // 친구 관계 생성 (80%는 수락됨, 20%는 대기중)
            for (Integer friendId : friendIds) {
                String status = random.nextInt(10) < 8 ? "ACCEPTED" : "PENDING";
                friendData.add(new Object[]{userId, friendId, status});
            }
        }
        
        // 배치로 친구 관계 삽입
        jdbcTemplate.batchUpdate(friendSql, friendData);
        
        log.info("✅ 친구 관계 생성 완료: {} 건", friendData.size());
    }
    
    /**
     * 소셜 상호작용 생성 (북마크, 좋아요)
     */
    @Transactional
    protected void generateSocialInteractions() {
        log.info("❤️ 소셜 상호작용 생성 중 (북마크, 좋아요)...");
        
        // 코스 북마크 생성 (사용자당 평균 3-10개)
        generateCourseBookmarks();
        
        // 코스 좋아요 생성 (코스당 평균 5-50개)
        generateCourseLikes();
        
        log.info("✅ 소셜 상호작용 생성 완료");
    }
    
    private void generateCourseBookmarks() {
        String sql = "INSERT INTO course_bookmark (user_id, recommended_course_id, bookmarked_at) VALUES (?, ?, ?)";
        
        List<Object[]> bookmarkData = new ArrayList<>();
        
        for (int userId = 1; userId <= 1000; userId++) {
            int bookmarkCount = 3 + random.nextInt(8); // 3-10개
            
            Set<Integer> courseIds = new HashSet<>();
            while (courseIds.size() < bookmarkCount) {
                int courseId = 1 + random.nextInt(8000);
                courseIds.add(courseId);
            }
            
            for (Integer courseId : courseIds) {
                LocalDateTime bookmarkedAt = LocalDateTime.now().minusDays(random.nextInt(90));
                bookmarkData.add(new Object[]{userId, courseId, Timestamp.valueOf(bookmarkedAt)});
            }
        }
        
        jdbcTemplate.batchUpdate(sql, bookmarkData);
        log.info("  📌 북마크: {} 건", bookmarkData.size());
    }
    
    private void generateCourseLikes() {
        String sql = "INSERT INTO course_like (user_id, recommended_course_id) VALUES (?, ?)";
        
        List<Object[]> likeData = new ArrayList<>();
        
        // 각 코스마다 랜덤한 좋아요 생성
        for (int courseId = 1; courseId <= 8000; courseId++) {
            int likeCount = 5 + random.nextInt(46); // 5-50개
            
            Set<Integer> userIds = new HashSet<>();
            while (userIds.size() < likeCount) {
                int userId = 1 + random.nextInt(1000);
                userIds.add(userId);
            }
            
            for (Integer userId : userIds) {
                likeData.add(new Object[]{userId, courseId});
            }
        }
        
        jdbcTemplate.batchUpdate(sql, likeData);
        log.info("  ❤️ 좋아요: {} 건", likeData.size());
    }

    // === 추천 코스 생성을 위한 유틸리티 메서드들 ===
    
    private double generateRecommendedCourseDistance(Level level) {
        // 추천 코스는 일반적으로 더 좋은 거리
        return switch (level) {
            case BEGINNER -> 2000 + random.nextDouble() * 3000;      // 2-5km
            case INTERMEDIATE -> 3000 + random.nextDouble() * 7000;   // 3-10km  
            case ADVANCED -> 5000 + random.nextDouble() * 15000;     // 5-20km
            case EXPERT -> 8000 + random.nextDouble() * 17000;       // 8-25km
            case MASTER -> 10000 + random.nextDouble() * 32000;      // 10-42km
        };
    }
    
    private String generateCourseTitle(double distance, int index) {
        String[] themes = {
            "한강 {} 코스", "남산 {} 트레일", "올림픽공원 {} 루트", "청계천 {} 코스",
            "여의도 {} 코스", "반포 {} 루트", "뚝섬 {} 트레일", "잠실 {} 코스",
            "상암 {} 루트", "용산 {} 코스", "성수 {} 트레일", "홍대 {} 루트"
        };
        
        String distanceStr = distance < 3000 ? "단거리" : 
                           distance < 8000 ? "중거리" :
                           distance < 15000 ? "장거리" : "마라톤";
        
        String theme = themes[index % themes.length];
        return String.format(theme, distanceStr);
    }
    
    private String generateCourseDescription(double distance, Level level) {
        String[] descriptions = {
            "초보자도 쉽게 완주할 수 있는 평지 코스입니다. 아름다운 풍경과 함께 가볍게 뛰어보세요!",
            "적당한 난이도의 코스로 체력 향상에 도움됩니다. 중간중간 경치 좋은 포인트들이 있어요.",
            "도전적인 언덕과 내리막이 있는 코스입니다. 실력 향상을 원하는 러너들에게 추천합니다.",
            "전문 러너를 위한 고난이도 코스입니다. 끝까지 완주하면 성취감이 정말 큽니다!",
            "마라톤 훈련에 최적화된 장거리 코스입니다. 페이스 조절이 중요한 코스예요."
        };
        
        return descriptions[random.nextInt(descriptions.length)];
    }
    
    private String generateRecommendedCourseGeoJson(double[] start, double[] end, double distance) {
        // 더 정교한 경로 (중간 포인트들 추가)
        int waypoints = Math.max(3, (int)(distance / 2000)); // 2km마다 중간 포인트
        StringBuilder coordinates = new StringBuilder();
        coordinates.append("[");
        
        for (int i = 0; i <= waypoints; i++) {
            double ratio = (double) i / waypoints;
            double lat = start[0] + (end[0] - start[0]) * ratio;
            double lng = start[1] + (end[1] - start[1]) * ratio;
            
            // 약간의 랜덤 변화 추가 (실제 길을 따라가는 느낌)
            lat += (random.nextDouble() - 0.5) * 0.001;
            lng += (random.nextDouble() - 0.5) * 0.001;
            
            if (i > 0) coordinates.append(",");
            coordinates.append(String.format("[%.6f,%.6f]", lng, lat));
        }
        
        coordinates.append("]");
        
        return String.format("""
            {"type":"LineString","coordinates":%s}
            """, coordinates.toString());
    }
    
    private String generateCourseImageUrl(int index) {
        // 실제 서비스라면 AWS S3 URL 형태로
        String[] imageTypes = {"sunrise", "park", "river", "trail", "city", "nature"};
        String imageType = imageTypes[index % imageTypes.length];
        return String.format("https://runrush-images.s3.ap-northeast-2.amazonaws.com/courses/%s/%d.jpg", 
                           imageType, (index % 100) + 1);
    }

    /**
     * 추천 코스를 따라 뛴 러닝 기록들 생성 (인기 코스 통계용)
     */
    @Transactional
    protected void generateCourseFollowingRecords() {
        log.info("🏃‍♂️ 추천 코스를 따라 뛴 러닝 기록들 생성 중...");
        
        // 인기 있는 추천 코스들 (상위 1000개)에 대해 추가 러닝 기록 생성
        String sql = """
            INSERT INTO running_record 
            (user_id, recommended_course_id, started_time, ended_time, total_distance, total_time, pace,
             start_latitude, start_longitude, end_latitude, end_longitude,
             start_location_name, end_location_name, path_geo_json, 
             is_deleted, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, false, ?)
            """;
        
        // 상위 1000개 추천 코스에 대해 각각 5-50개의 완주 기록 생성
        for (int courseId = 1; courseId <= 1000; courseId++) {
            int followingCount = 5 + random.nextInt(46); // 5-50개
            
            List<Object[]> recordData = new ArrayList<>();
            
            for (int i = 0; i < followingCount; i++) {
                // 랜덤 사용자 선택
                long userId = 1 + random.nextInt(1000);
                
                // 현실적인 러닝 시간 생성 (최근 3개월)
                LocalDateTime startTime = generateRealisticRunningTime(courseId * followingCount + i);
                
                // 사용자 레벨에 따른 현실적인 거리와 페이스
                Level userLevel = getUserLevel(userId);
                double distance = generateRecommendedCourseDistance(userLevel);
                double pace = generateRealisticPace(userLevel, distance);
                long totalTime = (long) (distance / 1000.0 * pace * 60); // 초 단위
                
                LocalDateTime endTime = startTime.plusSeconds(totalTime);
                
                // 서울 지역 GPS 좌표
                double[] startCoords = generateSeoulCoordinates();
                double[] endCoords = generateNearbyCoordinates(startCoords, distance);
                
                String startLocation = generateLocationName();
                String endLocation = distance > 3000 ? generateLocationName() : startLocation;
                
                // 간단한 GeoJSON 생성
                String geoJson = generateSimpleGeoJson(startCoords, endCoords);
                
                recordData.add(new Object[]{
                    userId,
                    (long) courseId, // recommended_course_id
                    Timestamp.valueOf(startTime),
                    Timestamp.valueOf(endTime),
                    distance,
                    totalTime,
                    pace,
                    startCoords[0], // latitude
                    startCoords[1], // longitude  
                    endCoords[0],
                    endCoords[1],
                    startLocation,
                    endLocation,
                    geoJson,
                    Timestamp.valueOf(startTime)
                });
            }
            
            // 배치로 삽입
            jdbcTemplate.batchUpdate(sql, recordData);
            
            // 진행률 로깅 (100개마다)
            if (courseId % 100 == 0) {
                log.info("진행률: {}/1000 코스 완료 ({:.1f}%)", courseId, (double) courseId / 1000 * 100);
            }
        }
        
        long totalFollowingRecords = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM running_record WHERE recommended_course_id IS NOT NULL", Long.class);
        log.info("✅ 추천 코스 완주 기록 생성 완료: {} 건", totalFollowingRecords);
    }

    /**
     * 생성된 데이터 요약 출력
     */
    private void printDataSummary() {
        log.info("📊 생성된 더미 데이터 요약:");
        log.info("  👥 사용자: {} 명", userRepository.count());
        log.info("  🏃 러닝 기록: {} 건", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM running_record", Long.class));
        log.info("  🎯 추천 코스: {} 건", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM recommended_course", Long.class));
        log.info("  👫 친구 관계: {} 건", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM friends", Long.class));
        log.info("  📌 코스 북마크: {} 건", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM course_bookmark", Long.class));
        log.info("  ❤️ 코스 좋아요: {} 건", jdbcTemplate.queryForObject("SELECT COUNT(*) FROM course_like", Long.class));
        log.info("  🏃‍♂️ 추천 코스 완주 기록: {} 건", 
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM running_record WHERE recommended_course_id IS NOT NULL", Long.class));
    }
}