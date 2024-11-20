package org.jucamdonaciones.swgdsjucambackend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rol_id;

    @Column(nullable = false, unique = true, length = 20)
    private String nombre;

    // Getters y Setters

    public Long getRol_id() {
        return rol_id;
    }

    public void setRol_id(Long rol_id) {
        this.rol_id = rol_id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }
}