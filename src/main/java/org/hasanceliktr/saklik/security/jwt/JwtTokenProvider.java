package org.hasanceliktr.saklik.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct; // @PostConstruct için import
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails; // UserDetails importu
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey; // SecretKey importu
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expirationMs}") // application.properties'e eklemiştik
    private int jwtExpirationMs;

    private SecretKey secretKeyInstance; // SecretKey'i burada saklayacağız

    @PostConstruct
    public void init() {
        if (this.jwtSecret == null || this.jwtSecret.isEmpty()) {
            String errorMessage = "JWT secret key cannot be null or empty. Please set 'app.jwt.secret' in application.properties with a valid Base64 encoded value.";
            logger.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }
        try {
            this.secretKeyInstance = Keys.hmacShaKeyFor(Decoders.BASE64.decode(this.jwtSecret));
            logger.info("JWT SecretKey successfully initialized from Base64 encoded secret.");
        } catch (IllegalArgumentException e) {
            String errorMessage = String.format(
                    "Invalid JWT secret key in 'app.jwt.secret': [%s]. It's NOT a valid Base64 encoded string or it's not strong enough for HS512. " +
                            "Please provide a Base64 encoded secret of at least 64 random bytes for HS512.",
                    this.jwtSecret
            );
            logger.error(errorMessage, e);
            throw new IllegalArgumentException(errorMessage, e);
        }
    }

    private SecretKey key() {
        if (this.secretKeyInstance == null) {
            // Bu durumun olmaması lazım eğer @PostConstruct doğru çalıştıysa ve hata fırlattıysa.
            throw new IllegalStateException("JWT SecretKey has not been initialized. Check JWT configuration and application startup logs.");
        }
        return this.secretKeyInstance;
    }

    public String generateToken(Authentication authentication) {
        // Authentication nesnesinin principal'ı UserDetails tipinde olmalı
        // (CustomUserDetailsService'den döndürdüğümüz org.springframework.security.core.userdetails.User nesnesi)
        UserDetails userPrincipal = (UserDetails) authentication.getPrincipal();
        String username = userPrincipal.getUsername(); // Bu, CustomUserDetailsService'de belirlediğimiz identifier (örn: user.getUsername() veya user.getEmail())

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(",")); // Rolleri virgülle ayrılmış string olarak ekleyelim

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles) // Rolleri claim olarak ekle
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key(), SignatureAlgorithm.HS512) // VEYA Jwts.SIG.HS512 (JJWT versiyonuna göre)
                .compact();
    }

    public String getUsernameFromJWT(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token) // Eğer parseClaimsJws hata veriyorsa parseSignedClaims deneyebilirsin
                .getPayload(); // veya .getBody() (JJWT versiyonuna göre)
        return claims.getSubject();
    }

    public boolean validateToken(String authToken) {
        try {
            Jwts.parser().verifyWith(key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty or invalid: {}", ex.getMessage());
        } catch (io.jsonwebtoken.security.SignatureException ex) { // JJWT 0.12.x ve sonrası için
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        }
        return false;
    }
}