package com.miDiario.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // Desactivar CSRF permite hacer POSTs desde JS fácilmente
                .csrf(csrf -> csrf.disable())

                // Permitir todo (incluido /api/chat/...)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                )
                // Configuraciones por defecto desactivadas para que no molesten
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}