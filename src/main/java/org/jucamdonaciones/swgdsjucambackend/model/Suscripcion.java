package org.jucamdonaciones.swgdsjucambackend.model;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "suscripciones")
public class Suscripcion {

    // Llave primaria interna auto-generada
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Aquí se almacena el ID de la suscripción que proviene de la API de Clip
    @Column(nullable = false, unique = true, length = 36)
    private String clipSubscriptionId;

    // Relación ManyToOne con Suscriptor
    @ManyToOne
    @JoinColumn(name = "suscriptor_id", nullable = false)
    private Suscriptor suscriptor;

    // Almacenamos el price_id de Clip, que coincide con el servicio
    @Column(nullable = false)
    private UUID servicioId;

    @Column(nullable = false)
    private LocalDateTime fechaInicio = LocalDateTime.now();

    @Column
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSuscripcion estado = EstadoSuscripcion.activa;

    @Column(nullable = false, length = 50)
    private String metodoPago;

    @Column(nullable = false, length = 100)
    private String transaccionId;

    @Column
    private LocalDateTime fechaUltimoPago;

    @Column
    private LocalDateTime proximoPago;

    public enum EstadoSuscripcion {
        activa, cancelada, suspendida
    }

    // Getters y Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getClipSubscriptionId() {
        return clipSubscriptionId;
    }

    public void setClipSubscriptionId(String clipSubscriptionId) {
        this.clipSubscriptionId = clipSubscriptionId;
    }

    public Suscriptor getSuscriptor() {
        return suscriptor;
    }

    public void setSuscriptor(Suscriptor suscriptor) {
        this.suscriptor = suscriptor;
    }

    public UUID getServicioId() {
        return servicioId;
    }

    public void setServicioId(UUID servicioId) {
        this.servicioId = servicioId;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public EstadoSuscripcion getEstado() {
        return estado;
    }

    public void setEstado(EstadoSuscripcion estado) {
        this.estado = estado;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getTransaccionId() {
        return transaccionId;
    }

    public void setTransaccionId(String transaccionId) {
        this.transaccionId = transaccionId;
    }

    public LocalDateTime getFechaUltimoPago() {
        return fechaUltimoPago;
    }

    public void setFechaUltimoPago(LocalDateTime fechaUltimoPago) {
        this.fechaUltimoPago = fechaUltimoPago;
    }

    public LocalDateTime getProximoPago() {
        return proximoPago;
    }

    public void setProximoPago(LocalDateTime proximoPago) {
        this.proximoPago = proximoPago;
    }
}