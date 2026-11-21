package com.rahmat.courseplatform.auth.service;

import com.rahmat.courseplatform.auth.RefreshToken;
import com.rahmat.courseplatform.auth.RefreshTokenRepository;
import com.rahmat.courseplatform.auth.dto.AuthResponse;
import com.rahmat.courseplatform.auth.dto.LoginRequest;
import com.rahmat.courseplatform.auth.dto.RefreshRequest;
import com.rahmat.courseplatform.user.Role;
import com.rahmat.courseplatform.user.User;
import com.rahmat.courseplatform.user.UserRepository;
import com.rahmat.courseplatform.user.dto.UserRegisterRequest;
import com.rahmat.courseplatform.user.mapper.UserMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final UserMapper userMapper;

    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtEncoder jwtEncoder,
            UserMapper userMapper,
            @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes,
            @Value("${app.jwt.refresh-token-days}") long refreshTokenDays) {

        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.userMapper = userMapper;
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    public AuthResponse register(UserRegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Email is already registered"
            );
        }

        User user = new User(
            request.email(),
            passwordEncoder.encode(request.password()),
            request.fullName(),
            Role.STUDENT
        );

        userRepository.save(user);

        return createTokens(user);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid email or password"
            ));

        if (!user.isEnabled()
                || user.getPasswordHash() == null
                || !passwordEncoder.matches(
                    request.password(), user.getPasswordHash())) {

            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid email or password"
            );
        }

        return createTokens(user);
    }

    public AuthResponse refresh(RefreshRequest request) {

        String hash = hashToken(request.refreshToken());

        RefreshToken oldToken = refreshTokenRepository
            .findByTokenHash(hash)
            .orElseThrow(() -> invalidRefreshToken());

        if (oldToken.isRevoked()
                || !oldToken.getExpiresAt().isAfter(Instant.now())) {

            throw invalidRefreshToken();
        }

        User user = oldToken.getUser();

        if (!user.isEnabled()) {
            throw invalidRefreshToken();
        }

        // Rotate: revoke the old token before issuing a new one.
        oldToken.setRevoked(true);

        return createTokens(user);
    }

    public void logout(RefreshRequest request) {

        String hash = hashToken(request.refreshToken());

        refreshTokenRepository.findByTokenHash(hash)
            .ifPresent(token -> token.setRevoked(true));
    }

    private AuthResponse createTokens(User user) {

        Instant now = Instant.now();
        Instant accessExpiry = now.plus(
            accessTokenMinutes, ChronoUnit.MINUTES
        );

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("courseplatform")
            .issuedAt(now)
            .expiresAt(accessExpiry)
            .subject(user.getEmail())
            .claim("userId", user.getId())
            .claim("role", user.getRole().name())
            .build();

        JwsHeader header = JwsHeader
            .with(MacAlgorithm.HS256)
            .build();

        String accessToken = jwtEncoder.encode(
            JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);

        String rawRefreshToken = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes);

        String refreshHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken(
            user,
            refreshHash,
            now.plus(refreshTokenDays, ChronoUnit.DAYS)
        );

        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
            accessToken,
            rawRefreshToken,
            "Bearer",
            accessTokenMinutes * 60,
            userMapper.toResponse(user)
        );
    }

    private String hashToken(String token) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                token.getBytes(StandardCharsets.UTF_8)
            );

            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(hash);

        } catch (Exception ex) {
            throw new IllegalStateException(
                "Unable to hash refresh token", ex
            );
        }
    }

    private ResponseStatusException invalidRefreshToken() {
        return new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Invalid or expired refresh token"
        );
    }
}