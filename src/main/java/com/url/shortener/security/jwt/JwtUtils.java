package com.url.shortener.security.jwt;

import com.url.shortener.service.UserDetailsImpl;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.stream.Collectors;

@Component
public class JwtUtils {
//    Authorization -> Bearer <Token>

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private String jwtExpirationMs;

    public String getJwtFromHeader(HttpServletRequest request)
    {
        String bearerToken = request.getHeader("Authorization");
        if(bearerToken != null && bearerToken.startsWith("Bearer ")){
            return bearerToken.substring(7);
        }
        return null;
    }

    public String generateToken(UserDetailsImpl userDetails){
        String username = userDetails.getUsername();
        String roles = userDetails.getAuthorities().stream()
                .map(authoriy -> authoriy.getAuthority())
                .collect(Collectors.joining(","));

        Instant now = Instant.now();
        Instant expiration = now.plus(Long.parseLong(jwtExpirationMs), ChronoUnit.MILLIS);

        return Jwts.builder()
                .subject(username)
                .claim("roles",roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(key())
                .compact();
    }

    public String getUserNameFromJwtToken(String token)
    {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }

    private Key key(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public boolean validateToken(String authToken)
    {
        try {
            Jwts.parser().verifyWith((SecretKey) key())
                    .build().parseSignedClaims(authToken);
            return true;
        } catch (JwtException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
