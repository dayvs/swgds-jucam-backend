package org.jucamdonaciones.swgdsjucambackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long usuario_id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false, length = 50)
    private String apellidos;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 256)
    private String password;

    @Column(nullable = false)
    private String estado = "activo";

    @Column(nullable = false)
    private Boolean requiere_cambio_contraseña = false;

    @Column
    private LocalDateTime fecha_ultimo_acceso;

    @Column
    private int intentos_fallidos = 0;

    @ManyToOne
    @JoinColumn(name = "rol_id", nullable = false)
    private Role rol;

    // Getters y Setters

    public Long getUsuario_id() {
        return usuario_id;
    }

    public void setUsuario_id(Long usuario_id) {
        this.usuario_id = usuario_id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Boolean getRequiere_cambio_contraseña() {
        return requiere_cambio_contraseña;
    }

    public void setRequiere_cambio_contraseña(Boolean requiere_cambio_contraseña) {
        this.requiere_cambio_contraseña = requiere_cambio_contraseña;
    }

    public LocalDateTime getFecha_ultimo_acceso() {
        return fecha_ultimo_acceso;
    }

    public void setFecha_ultimo_acceso(LocalDateTime fecha_ultimo_acceso) {
        this.fecha_ultimo_acceso = fecha_ultimo_acceso;
    }

    public int getIntentos_fallidos() {
        return intentos_fallidos;
    }

    public void setIntentos_fallidos(int intentos_fallidos) {
        this.intentos_fallidos = intentos_fallidos;
    }

    public Role getRol() {
        return rol;
    }

    public void setRol(Role rol) {
        this.rol = rol;
    }
}