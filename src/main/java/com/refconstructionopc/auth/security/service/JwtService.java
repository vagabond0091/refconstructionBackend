package com.refconstructionopc.auth.security.service;


import com.refconstructionopc.dto.UserDTO;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);
//    private static final long MIN_24H_MS = 24L * 60 * 60 * 1000; // 24 hours
    private static final long MIN_24H_MS = 60L * 60 * 1000;
    @Value("${jwt.secret}")
    private String secretKey;

    // Optional override (e.g., 2 days = 172800000). If lower than 24h, we bump it to 24h.
    @Value("${jwt.expiration-ms:3600000}")
    private long configuredExpirationMs;


//    @PostConstruct
//    void initSecretIfMissing() {
//        if (secretKey == null || secretKey.isBlank()) {
//            byte[] key = new byte[32]; // 256-bit
//            new SecureRandom().nextBytes(key);
//            String generated = Base64.getEncoder().encodeToString(key);
//            secretKey = generated;
//
//            logger.warn("⚠ No jwt.secret provided. Generated a temporary one for this run.");
//            logger.warn("👉 Add this to your application.properties to persist it:");
//            logger.warn("jwt.secret={}", generated);
//        }
//    }
    /** Subject-only token (kept for your existing usage). */
    public String generateToken(String username) {
        return generateTokenWithClaims(username, new HashMap<>());
    }

    /** Token with user details embedded in claims. */
    public String generateToken(UserDTO user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("firstName", user.getFirstName());
        claims.put("lastName", user.getLastName());
        claims.put("role", user.getRole().toString());
        return generateTokenWithClaims(user.getEmail(), claims);
    }

    private String generateTokenWithClaims(String subject, Map<String, Object> claims) {
        long nowMs = System.currentTimeMillis();
        long expMs = getEffectiveExpirationMs();

        Date issuedAt = new Date(nowMs);
        Date expiresAt = new Date(nowMs + expMs);

        // Convenience for clients that want an explicit epoch-seconds expiry
        claims.put("expiresAt", (nowMs + expMs) / 1000);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(issuedAt)
                .expiration(expiresAt)                 // standard "exp"
                .signWith(getKey())                    // JJWT 0.12.x infers HS alg from key
                .compact();
    }

    private long getEffectiveExpirationMs() {
        return Math.max(configuredExpirationMs, MIN_24H_MS);
    }

    public long getEffectiveExpirationSeconds() {
        return getEffectiveExpirationMs() / 1000;
    }
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        logger.info(" Extracted Username from Token: {}", username);
        logger.info(" Expected Username: {}", userDetails.getUsername());

        boolean isExpired = extractExpirationDate(token).before(new Date());
        if (isExpired) {
            logger.info(" Token has expired!");
            return false;
        }
        boolean isValid = username != null && username.equals(userDetails.getUsername());
        logger.info(" Token Validity: {}", isValid);
        return isValid;
    }

    private Date extractExpirationDate(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())   // JJWT 0.12.x
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
