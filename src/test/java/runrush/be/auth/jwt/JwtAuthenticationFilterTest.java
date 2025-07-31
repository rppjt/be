package runrush.be.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import runrush.be.auth.model.UserPrincipal;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import runrush.be.common.exception.ErrorResponse;
import runrush.be.user.domain.User;
import runrush.be.user.repository.UserRepository;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private PrintWriter writer;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("OPTIONS 요청시 인증을 건너뛰고 필터 체인 계속 진행")
    void doFilterInternal_OptionsRequest_SkipsAuth() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("OPTIONS");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).validateToken(any());
        verifyNoInteractions(userRepository);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("유효한 토큰으로 인증 성공")
    void doFilterInternal_ValidToken_SetsAuthentication() throws ServletException, IOException {
        // given
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("token")).thenReturn("test@example.com");

        User testUser = User.builder()
                .email("test@example.com")
                .name("테스트 유저")
                .nickname("테스트")
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // when
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        // then
        verify(filterChain).doFilter(request, response);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);

        UserPrincipal userPrincipal = (UserPrincipal) auth.getPrincipal();
        assertThat(userPrincipal.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("토큰이 없을 때 필터 체인 계속 진행")
    void doFilterInternal_NoToken_ContinuesFilter() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);

        verifyNoInteractions(jwtTokenProvider);

        verifyNoInteractions(userRepository);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("잘못된 토큰으로 인증 실패 - BusinessException")
    void doFilterInternal_InvalidToken_HandlesBusinessException() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalidtoken");
        when(jwtTokenProvider.validateToken("invalidtoken"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_TOKEN));
        when(response.getWriter()).thenReturn(writer);
        when(objectMapper.writeValueAsString(any(ErrorResponse.class))).thenReturn("{\"error\":\"invalid_token\"}");
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        
        verify(response).setStatus(ErrorCode.INVALID_TOKEN.getStatus().value());
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(writer).write("{\"error\":\"invalid_token\"}");
        
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("예상치 못한 예외 발생시 오류 처리")
    void doFilterInternal_UnexpectedException_HandlesError() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtTokenProvider.validateToken("token")).thenThrow(new RuntimeException("예상치 못한 오류"));
        when(response.getWriter()).thenReturn(writer);
        when(objectMapper.writeValueAsString(any(ErrorResponse.class))).thenReturn("{\"error\":\"invalid_token\"}");
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        
        verify(response).setStatus(ErrorCode.INVALID_TOKEN.getStatus().value());
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(writer).write("{\"error\":\"invalid_token\"}");
        
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("사용자를 찾을 수 없는 경우 RuntimeException")
    void doFilterInternal_UserNotFound_ThrowsRuntimeException() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(jwtTokenProvider.validateToken("token")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("token")).thenReturn("notfound@example.com");
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        when(response.getWriter()).thenReturn(writer);
        when(objectMapper.writeValueAsString(any(ErrorResponse.class))).thenReturn("{\"error\":\"invalid_token\"}");
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        
        verify(response).setStatus(ErrorCode.INVALID_TOKEN.getStatus().value());
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(writer).write("{\"error\":\"invalid_token\"}");
        
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("유효한 Bearer 토큰으로 인증 처리 - extractToken 간접 테스트")
    void doFilterInternal_ValidBearerToken_ExtractsTokenCorrectly() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer mytoken123");
        when(jwtTokenProvider.validateToken("mytoken123")).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken("mytoken123")).thenReturn("test@example.com");
        
        User testUser = User.builder()
                .email("test@example.com")
                .name("테스트 유저")
                .nickname("테스트")
                .build();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        verify(jwtTokenProvider).validateToken("mytoken123"); // Bearer 제거 확인
        verify(jwtTokenProvider).getEmailFromToken("mytoken123");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Authorization 헤더가 없을 때 토큰 추출 실패 - 간접 테스트")
    void doFilterInternal_NoAuthHeader_SkipsTokenValidation() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn(null);
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        verifyNoInteractions(jwtTokenProvider);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Bearer가 아닌 토큰일 때 추출 실패 - 간접 테스트")
    void doFilterInternal_NonBearerToken_SkipsTokenValidation() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        verifyNoInteractions(jwtTokenProvider);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("JWT 오류 처리 동작 확인 - handleJwtError 간접 테스트")
    void doFilterInternal_JwtError_HandlesErrorCorrectly() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("Authorization")).thenReturn("Bearer invalidtoken");
        when(request.getRequestURI()).thenReturn("/api/test");
        when(jwtTokenProvider.validateToken("invalidtoken"))
                .thenThrow(new BusinessException(ErrorCode.INVALID_TOKEN));
        when(response.getWriter()).thenReturn(writer);
        when(objectMapper.writeValueAsString(any(ErrorResponse.class)))
                .thenReturn("{\"code\":\"INVALID_TOKEN\",\"message\":\"유효하지 않은 토큰입니다\",\"path\":\"/api/test\"}");
        
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        
        verify(response).setStatus(ErrorCode.INVALID_TOKEN.getStatus().value());
        verify(response).setContentType("application/json;charset=UTF-8");
        
        verify(writer).write("{\"code\":\"INVALID_TOKEN\",\"message\":\"유효하지 않은 토큰입니다\",\"path\":\"/api/test\"}");
        
        verify(objectMapper).writeValueAsString(any(ErrorResponse.class));
        
        verify(filterChain, never()).doFilter(request, response);
    }
}