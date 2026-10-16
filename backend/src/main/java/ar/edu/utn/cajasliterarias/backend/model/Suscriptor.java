package ar.edu.utn.cajasliterarias.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "suscriptor")
@Data
public class Suscriptor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String email;
    private String direccion;
    private LocalDate fechaRegistro;

    // Hash BCrypt. Nullable: los suscriptores creados con el alta vieja no tienen password y no pueden loguearse.
    // @JsonIgnore: Pago y Suscripcion contienen un Suscriptor y se devuelven como JSON; el hash nunca tiene que salir.
    @JsonIgnore
    @ToString.Exclude
    @Column(name = "password_hash", length = 100)
    private String passwordHash;
}