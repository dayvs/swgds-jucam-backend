package org.jucamdonaciones.swgdsjucambackend.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@Table(name = "donaciones")
public class Donacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long donacionId;

    @ManyToOne
    @JoinColumn(name = "donador_id")
    private Donador donador;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDateTime fechaDonacion = LocalDateTime.now();

    @Column(nullable = false, length = 50)
    private String metodoPago;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Estado estado;

    @Column(nullable = false, length = 100)
    private String transaccionId;

    @Column(nullable = false)
    private Boolean anonimato = false;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Frecuencia frecuencia = Frecuencia.UNICA;

    @Column(nullable = true)
    private Integer suscripcionId;

    // Definición de los enums
    public enum Estado {
        exitoso, fallido
    }

    public enum Frecuencia {
        UNICA, MENSUAL, TRIMESTRAL, ANUAL
    }

    // Getters y Setters
    public Long getDonacionId() {
        return donacionId;
    }

    public void setDonacionId(Long donacionId) {
        this.donacionId = donacionId;
    }

    public Donador getDonador() {
        return donador;
    }

    public void setDonador(Donador donador) {
        this.donador = donador;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public LocalDateTime getFechaDonacion() {
        return fechaDonacion;
    }

    public void setFechaDonacion(LocalDateTime fechaDonacion) {
        this.fechaDonacion = fechaDonacion;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public String getTransaccionId() {
        return transaccionId;
    }

    public void setTransaccionId(String transaccionId) {
        this.transaccionId = transaccionId;
    }

    public Boolean getAnonimato() {
        return anonimato;
    }

    public void setAnonimato(Boolean anonimato) {
        this.anonimato = anonimato;
    }

    public Frecuencia getFrecuencia() {
        return frecuencia;
    }

    public void setFrecuencia(Frecuencia frecuencia) {
        this.frecuencia = frecuencia;
    }

    public Integer getSuscripcionId() {
        return suscripcionId;
    }

    public void setSuscripcionId(Integer suscripcionId) {
        this.suscripcionId = suscripcionId;
    }
}