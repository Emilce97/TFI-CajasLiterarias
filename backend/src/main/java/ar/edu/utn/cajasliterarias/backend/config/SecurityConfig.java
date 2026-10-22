package ar.edu.utn.cajasliterarias.backend.config;

import java.io.IOException;

import ar.edu.utn.cajasliterarias.backend.security.JwtAuthFilter;
import ar.edu.utn.cajasliterarias.backend.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;

    @Bean
    SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // públicas (visitante)
                        .requestMatchers("/api/auth/login", "/api/auth/registro").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categorias").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ediciones", "/api/ediciones/*/curadurias").permitAll()
                        // suscriptor
                        .requestMatchers(HttpMethod.POST, "/api/suscripciones", "/api/pagos").hasRole("SUSCRIPTOR")
                        .requestMatchers(HttpMethod.GET, "/api/suscripciones/mis-suscripciones", "/api/suscriptores/mi-perfil").hasRole("SUSCRIPTOR")
                        .requestMatchers(HttpMethod.PATCH, "/api/suscripciones/**", "/api/suscriptores/mi-perfil").hasRole("SUSCRIPTOR")
                        // administradora
                        .requestMatchers(HttpMethod.POST, "/api/ediciones", "/api/ediciones/*/cierre").hasRole("ADMIN")
                        .requestMatchers("/api/pagos/pendientes", "/api/pagos/*/validar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/ediciones/*/demanda").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/demanda/*/recepcion").hasRole("ADMIN")
                        // lo que no está listado arriba es solo de la administradora
                        .anyRequest().hasRole("ADMIN"))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> escribir(res, 401, "No autenticado", "Iniciá sesión para continuar"))
                        .accessDeniedHandler((req, res, ex) -> escribir(res, 403, "Sin permisos", "No tenés permisos para esta operación")))
                .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // Misma forma que ErrorResponse del proyecto (status, error, message)
    private void escribir(HttpServletResponse res, int status, String error, String msg) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error + "\",\"message\":\"" + msg + "\"}");
    }
}
