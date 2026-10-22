package ar.edu.utn.cajasliterarias.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "administradora")
@Data
public class Administradora {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String email;

    @JsonIgnore
    @ToString.Exclude
    @Column(name = "password_hash", length = 100)
    private String passwordHash;
}