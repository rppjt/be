package runrush.be.performance.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import runrush.be.coursebookmark.domain.CourseBookmark;
import runrush.be.coursebookmark.repository.CourseBookmarkRepository;
import runrush.be.courselike.repository.CourseLikeRepository;
import runrush.be.runningrecord.domain.RunningRecord;
import runrush.be.runningrecord.repository.RunningRecordRepository;
import runrush.be.runningrecord.service.RunningRecordService;
import runrush.be.stats.dto.PopularRecommendedCourseResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 성능 테스트용 컨트롤러
 * 
 * 인덱스 최적화 전후 성능 비교를 위한 API 엔드포인트들
 * 그라파나의 http_server_requests_seconds 메트릭으로 응답 시간 자동 측정됨
 */
@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Slf4j
public class PerformanceTestController {
    
    private final RunningRecordService runningRecordService;
    private final RunningRecordRepository runningRecordRepository;
    private final CourseBookmarkRepository courseBookmarkRepository;
    private final CourseLikeRepository courseLikeRepository;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 사용자별 러닝 기록 조회 (가장 무거운 쿼리)
     * 인덱스 효과가 가장 극명하게 나타날 것으로 예상
     */
    @GetMapping("/test/user-records/{userId}")
    public ResponseEntity<Map<String, Object>> testUserRecords(@PathVariable Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("🔍 [PERFORMANCE TEST] 사용자 {} 기록 조회 시작", userId);
        
        try {
            List<RunningRecord> records = runningRecordRepository.findByUserIdAndIsDeletedFalse(userId);
            
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.info("✅ [PERFORMANCE TEST] 사용자 {} 기록 조회 완료: {}건, {:.3f}초 소요", 
                userId, records.size(), executionTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("recordCount", records.size());
            response.put("executionTimeSeconds", executionTime);
            response.put("timestamp", LocalDateTime.now());
            response.put("testType", "user-records");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.error("❌ [PERFORMANCE TEST] 사용자 {} 기록 조회 실패: {:.3f}초 소요", 
                userId, executionTime, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "executionTimeSeconds", executionTime
            ));
        }
    }
    
    /**
     * 월별 기록 조회 테스트
     * 날짜 범위 검색 성능 측정
     */
    @GetMapping("/test/monthly-records/{userId}")
    public ResponseEntity<Map<String, Object>> testMonthlyRecords(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "2024") int year,
            @RequestParam(defaultValue = "8") int month) {
        
        long startTime = System.currentTimeMillis();
        log.info("📅 [PERFORMANCE TEST] 사용자 {} 월별 기록 조회 시작 ({}/{})", userId, year, month);
        
        try {
            List<RunningRecord> records = runningRecordRepository.findMonthlyRecords(userId, year, month);
            
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.info("✅ [PERFORMANCE TEST] 사용자 {} 월별 기록 조회 완료: {}건, {:.3f}초 소요", 
                userId, records.size(), executionTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("year", year);
            response.put("month", month);
            response.put("recordCount", records.size());
            response.put("executionTimeSeconds", executionTime);
            response.put("timestamp", LocalDateTime.now());
            response.put("testType", "monthly-records");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.error("❌ [PERFORMANCE TEST] 월별 기록 조회 실패: {:.3f}초 소요", executionTime, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "executionTimeSeconds", executionTime
            ));
        }
    }
    
    /**
     * 통계 쿼리 테스트
     * 복잡한 집계 쿼리 성능 측정
     */
    @GetMapping("/test/popular-courses")
    public ResponseEntity<Map<String, Object>> testPopularCourses() {
        long startTime = System.currentTimeMillis();
        log.info("📊 [PERFORMANCE TEST] 인기 코스 통계 조회 시작");
        
        try {
            List<PopularRecommendedCourseResponse> stats = runningRecordRepository.findPopularRecommendedCourseStats();
            
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.info("✅ [PERFORMANCE TEST] 인기 코스 통계 조회 완료: {}건, {:.3f}초 소요", 
                stats.size(), executionTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("courseCount", stats.size());
            response.put("executionTimeSeconds", executionTime);
            response.put("timestamp", LocalDateTime.now());
            response.put("testType", "popular-courses");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.error("❌ [PERFORMANCE TEST] 통계 쿼리 실패: {:.3f}초 소요", executionTime, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "executionTimeSeconds", executionTime
            ));
        }
    }
    
    /**
     * 인기 코스 통계 (별칭 엔드포인트)
     */
    @GetMapping("/courses/popular")
    public ResponseEntity<Map<String, Object>> getPopularCourses() {
        return testPopularCourses();
    }
    
    /**
     * 친구 네트워크 조회 테스트
     */
    @GetMapping("/friends/{userId}/network")
    public ResponseEntity<Map<String, Object>> testFriendNetwork(@PathVariable Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("👥 [PERFORMANCE TEST] 사용자 {} 친구 네트워크 조회 시작", userId);
        
        try {
            // 친구 관계 조회 (ACCEPTED 상태만)
            String sql = "SELECT COUNT(*) FROM friends WHERE (requester_id = ? OR target_id = ?) AND friend_status = 'ACCEPTED'";
            Long friendCount = jdbcTemplate.queryForObject(sql, Long.class, userId, userId);
            
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.info("✅ [PERFORMANCE TEST] 친구 네트워크 조회 완료: {}명, {:.3f}초 소요", 
                friendCount, executionTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("friendCount", friendCount);
            response.put("executionTimeSeconds", executionTime);
            response.put("timestamp", LocalDateTime.now());
            response.put("testType", "friend-network");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.error("❌ [PERFORMANCE TEST] 친구 네트워크 조회 실패: {:.3f}초 소요", executionTime, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "executionTimeSeconds", executionTime
            ));
        }
    }

    /**
     * 종합적인 성능 테스트 (새로운 완전한 데이터셋 테스트)
     */
    @GetMapping("/comprehensive-test")
    public ResponseEntity<Map<String, Object>> runComprehensiveTest() {
        log.info("🚀 [COMPREHENSIVE TEST] 완전한 데이터셋 성능 테스트 시작");
        
        Map<String, Object> results = new HashMap<>();
        results.put("startTime", LocalDateTime.now());
        
        // 1. 러닝 기록 테스트 (3명)
        for (Long userId : List.of(1L, 50L, 100L)) {
            try {
                ResponseEntity<Map<String, Object>> result = testUserRecords(userId);
                results.put("runningRecords_user" + userId, result.getBody());
            } catch (Exception e) {
                results.put("runningRecords_user" + userId + "_error", e.getMessage());
            }
        }
        
        // 2. 월별 기록 테스트
        try {
            ResponseEntity<Map<String, Object>> result = testMonthlyRecords(1L, 2024, 8);
            results.put("monthlyRecords", result.getBody());
        } catch (Exception e) {
            results.put("monthlyRecords_error", e.getMessage());
        }
        
        // 3. 인기 코스 통계
        try {
            ResponseEntity<Map<String, Object>> result = testPopularCourses();
            results.put("popularCourses", result.getBody());
        } catch (Exception e) {
            results.put("popularCourses_error", e.getMessage());
        }
        
        // 4. 친구 네트워크 테스트 (3명)
        for (Long userId : List.of(1L, 25L, 75L)) {
            try {
                ResponseEntity<Map<String, Object>> result = testFriendNetwork(userId);
                results.put("friendNetwork_user" + userId, result.getBody());
            } catch (Exception e) {
                results.put("friendNetwork_user" + userId + "_error", e.getMessage());
            }
        }
        
        results.put("endTime", LocalDateTime.now());
        results.put("summary", Map.of(
            "totalTests", 9,
            "datasetInfo", Map.of(
                "users", 1000,
                "runningRecords", 1000000,
                "recommendedCourses", 8000,
                "friendRelationships", "~10000",
                "courseBookmarks", "~6500",
                "courseLikes", "~200000"
            )
        ));
        
        log.info("✅ [COMPREHENSIVE TEST] 완전한 데이터셋 성능 테스트 완료");
        return ResponseEntity.ok(results);
    }
    
    /**
     * 코스 북마크 조회 테스트
     */
    @GetMapping("/course-bookmarks/user/{userId}")
    public ResponseEntity<Map<String, Object>> testCourseBookmarks(@PathVariable Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("📚 [PERFORMANCE TEST] 사용자 {} 코스 북마크 조회 시작", userId);
        
        try {
            // 1. 사용자의 북마크 목록 조회
            List<CourseBookmark> bookmarks = courseBookmarkRepository.findWithCourseByUserId(userId);
            
            // 2. 중복 체크 성능도 테스트 (EXISTS 쿼리)
            boolean hasBookmark = courseBookmarkRepository.existsByUserIdAndRecommendedCourseId(userId, 1L);
            
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.info("✅ [PERFORMANCE TEST] 사용자 {} 코스 북마크 조회 완료: {}건, EXISTS: {}, {:.3f}초 소요", 
                userId, bookmarks.size(), hasBookmark, executionTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("bookmarkCount", bookmarks.size());
            response.put("hasBookmarkSample", hasBookmark);
            response.put("executionTimeSeconds", executionTime);
            response.put("timestamp", LocalDateTime.now());
            response.put("testType", "course-bookmarks");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.error("❌ [PERFORMANCE TEST] 코스 북마크 조회 실패: {:.3f}초 소요", executionTime, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "executionTimeSeconds", executionTime
            ));
        }
    }

    /**
     * 코스 좋아요 조회 테스트
     */
    @GetMapping("/course-likes/user/{userId}")
    public ResponseEntity<Map<String, Object>> testCourseLikes(@PathVariable Long userId) {
        long startTime = System.currentTimeMillis();
        log.info("❤️ [PERFORMANCE TEST] 사용자 {} 코스 좋아요 조회 시작", userId);
        
        try {
            // 1. 좋아요 여부 확인 (EXISTS 쿼리)
            boolean hasLike = courseLikeRepository.existsByUserIdAndRecommendedCourseId(userId, 1L);
            
            // 2. 특정 코스의 좋아요 개수 조회
            long likeCount = courseLikeRepository.countByRecommendedCourseId(1L);
            
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.info("✅ [PERFORMANCE TEST] 사용자 {} 코스 좋아요 조회 완료: EXISTS: {}, 코스1 좋아요: {}개, {:.3f}초 소요", 
                userId, hasLike, likeCount, executionTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("userId", userId);
            response.put("hasLikeSample", hasLike);
            response.put("course1LikeCount", likeCount);
            response.put("executionTimeSeconds", executionTime);
            response.put("timestamp", LocalDateTime.now());
            response.put("testType", "course-likes");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            double executionTime = (endTime - startTime) / 1000.0;
            
            log.error("❌ [PERFORMANCE TEST] 코스 좋아요 조회 실패: {:.3f}초 소요", executionTime, e);
            
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getMessage(),
                "executionTimeSeconds", executionTime
            ));
        }
    }

    /**
     * 전체 성능 테스트 실행 (기존 테스트)
     */
    @GetMapping("/test/all")
    public ResponseEntity<Map<String, Object>> runAllTests() {
        log.info("🚀 [PERFORMANCE TEST] 전체 성능 테스트 시작");
        
        Map<String, Object> results = new HashMap<>();
        results.put("startTime", LocalDateTime.now());
        
        // 사용자 1, 10, 100 기록 조회 테스트
        for (Long userId : List.of(1L, 10L, 100L)) {
            try {
                ResponseEntity<Map<String, Object>> result = testUserRecords(userId);
                results.put("userRecords_" + userId, result.getBody());
            } catch (Exception e) {
                results.put("userRecords_" + userId + "_error", e.getMessage());
            }
        }
        
        // 월별 기록 조회 테스트
        try {
            ResponseEntity<Map<String, Object>> result = testMonthlyRecords(1L, 2024, 8);
            results.put("monthlyRecords", result.getBody());
        } catch (Exception e) {
            results.put("monthlyRecords_error", e.getMessage());
        }
        
        // 통계 쿼리 테스트
        try {
            ResponseEntity<Map<String, Object>> result = testPopularCourses();
            results.put("popularCourses", result.getBody());
        } catch (Exception e) {
            results.put("popularCourses_error", e.getMessage());
        }
        
        results.put("endTime", LocalDateTime.now());
        
        log.info("✅ [PERFORMANCE TEST] 전체 성능 테스트 완료");
        return ResponseEntity.ok(results);
    }
}