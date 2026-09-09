package ar.edu.utn.cajasliterarias.backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categoria")
@Data
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Misterio/Terror, Romance, Narrativa/Drama, Sorpresa
    private String nombre;
}