package org.jucamdonaciones.swgdsjucambackend.repository;

import java.time.LocalDateTime;

import org.jucamdonaciones.swgdsjucambackend.model.Donacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DonacionRepository extends JpaRepository<Donacion, Long> {
    List<Donacion> findByFechaDonacionBetween(LocalDateTime start, LocalDateTime end);
}