package org.example.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.example.service.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        
        // Если заголовок авторизации отсутствует или не содержит Bearer токен, пропускаем этот фильтр
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Извлекаем JWT из заголовка
        jwt = authHeader.substring(7);
        
        try {
            // Извлекаем email пользователя из JWT
            userEmail = jwtService.extractUsername(jwt);
            
            // Проверяем, что аутентификация еще не была выполнена
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Загружаем данные пользователя из базы
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                
                // Проверяем валидность токена
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Создаем объект аутентификации и устанавливаем его в SecurityContext
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                            
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // В случае ошибки при обработке токена, продолжаем цепочку фильтров без установки аутентификации
            logger.error("Не удалось установить аутентификацию: " + e.getMessage());
        }
        
        // Продолжаем цепочку фильтров
        filterChain.doFilter(request, response);
    }
} 