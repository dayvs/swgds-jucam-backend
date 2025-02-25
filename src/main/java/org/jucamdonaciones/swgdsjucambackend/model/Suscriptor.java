package org.jucamdonaciones.swgdsjucambackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "suscriptores")
public class Suscriptor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer suscriptorId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 15)
    private String telefono;

    @Column(nullable = false)
    private Boolean consentimientoDatos = true;

    @Column(nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    // Getters y Setters

    public Integer getSuscriptorId() {
        return suscriptorId;
    }

    public void setSuscriptorId(Integer suscriptorId) {
        this.suscriptorId = suscriptorId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }
  
    public void setEmail(String email) {
        this.email = email;
    }
  
    public String getTelefono() {
        return telefono;
    }
  
    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }
  
    public Boolean getConsentimientoDatos() {
        return consentimientoDatos;
    }
  
    public void setConsentimientoDatos(Boolean consentimientoDatos) {
        this.consentimientoDatos = consentimientoDatos;
    }
  
    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }
  
    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}