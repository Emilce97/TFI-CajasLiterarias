package ar.edu.utn.cajasliterarias.backend.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "clave-de-prueba-de-al-menos-32-caracteres-123";
    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Test
    void tokenGenerado_seValidaYDevuelveEmailYRol() {
        String token = jwtService.generar("admin@test.com", "ADMIN");

        JwtClaims claims = jwtService.validar(token);

        assertThat(claims).isNotNull();
        assertThat(claims.email()).isEqualTo("admin@test.com");
        assertThat(claims.rol()).isEqualTo("ADMIN");
    }

    @Test
    void tokenAlterado_devuelveNull() {
        String[] partes = jwtService.generar("admin@test.com", "ADMIN").split("\\.");
        String alterado = partes[0] + "." + partes[1] + "A." + partes[2];

        assertThat(jwtService.validar(alterado)).isNull();
    }

    @Test
    void tokenVencido_devuelveNull() {
        JwtService vencido = new JwtService(SECRET, -1); // vence antes de nacer
        String token = vencido.generar("admin@test.com", "ADMIN");

        assertThat(jwtService.validar(token)).isNull();
    }

    @Test
    void tokenFirmadoConOtroSecret_devuelveNull() {
        JwtService otro = new JwtService("otro-secret-distinto-de-al-menos-32-caracteres", 60);
        String token = otro.generar("admin@test.com", "ADMIN");

        assertThat(jwtService.validar(token)).isNull();
    }

    @Test
    void rolDesconocido_devuelveNull() {
        String token = jwtService.generar("admin@test.com", "OTRO");

        assertThat(jwtService.validar(token)).isNull();
    }

    @Test
    void textoQueNoEsToken_devuelveNull() {
        assertThat(jwtService.validar("esto-no-es-un-token")).isNull();
        assertThat(jwtService.validar("")).isNull();
    }

    @Test
    void secretDemasiadoCorto_lanzaError() {
        assertThatThrownBy(() -> new JwtService("corto", 60))
                .isInstanceOf(IllegalStateException.class);
    }
}