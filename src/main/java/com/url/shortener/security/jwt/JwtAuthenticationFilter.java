package com.url.shortener.security.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtTokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Value("${frontend.url}")
    private String frontend_Url;

    //Setting the header
    private void setCorsHeaders(HttpServletResponse response) {
        response.setHeader("Access-Control-Allow-Origin", frontend_Url);
        response.setHeader("Access-Control-Allow-Credentials", "true");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try{
//            Get Jwt from header
//            validate token
//            if valid Get User Details
//            get user name -> load User -> set the auth context
            String jwt = jwtTokenProvider.getJwtFromHeader(request);

            if(jwt != null && jwtTokenProvider.validateToken(jwt))
            {
                String username = jwtTokenProvider.getUserNameFromJwtToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if(userDetails != null)
                {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
//
        }
        catch (io.jsonwebtoken.ExpiredJwtException e) {
            // Catch Expired JWT and return 401 with a specific message
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            setCorsHeaders(response);
            response.getWriter().write(e.getMessage());
            return;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            // Catch Invalid JWT signature and return 401 with a specific message
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            setCorsHeaders(response);
            response.getWriter().write(e.getMessage());
            return;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            // Catch malformed JWT and return 401 with a specific message
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            setCorsHeaders(response);
            response.getWriter().write(e.getMessage());
            return;
        } catch (JwtException | IllegalArgumentException e) {
            // Catch general JWT validation exceptions and send response
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            setCorsHeaders(response);
            response.getWriter().write("JWT validation failed: " + e.getMessage());
            return;
        }
        catch (Exception e) {
            // Catch any other exceptions and return a generic 401 Unauthorized
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            setCorsHeaders(response);
            response.getWriter().write("Authentication failed: " + e.getMessage());
            return;
        }

        filterChain.doFilter(request,response);
    }
}
