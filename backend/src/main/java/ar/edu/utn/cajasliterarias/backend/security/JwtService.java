package ar.edu.utn.cajasliterarias.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;

@Service
public class JwtService {

    private static final Set<String> ROLES_VALIDOS = Set.of("ADMIN", "SUSCRIPTOR");
    private final SecretKey key;
    private final long expMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expMinutes) {
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expMinutes = expMinutes;
    }

    public String generar(String email, String rol) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("rol", rol)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** Devuelve email y rol si el token es válido, o null. */
    public JwtClaims validar(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            String rol = c.get("rol", String.class);
            if (rol == null || !ROLES_VALIDOS.contains(rol)) return null;
            return new JwtClaims(c.getSubject(), rol);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}
