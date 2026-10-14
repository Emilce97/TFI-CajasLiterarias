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
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
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
                        .requestMatchers("/api/auth/login").permitAll()
                        // públicas por ahora (la guía 2 las reemplaza por reglas de SUSCRIPTOR)
                        .requestMatchers(HttpMethod.GET, "/api/categorias").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/ediciones", "/api/ediciones/*/curadurias").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/suscripciones", "/api/pagos").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/suscripciones/**").permitAll()
                        // administración
                        .requestMatchers(HttpMethod.POST, "/api/ediciones", "/api/ediciones/*/cierre").hasRole("ADMIN")
                        .requestMatchers("/api/pagos/pendientes", "/api/pagos/*/validar").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/ediciones/*/demanda").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/demanda/*/recepcion").hasRole("ADMIN")
                        .anyRequest().authenticated())
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
