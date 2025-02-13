package org.jucamdonaciones.swgdsjucambackend.repository;

import org.jucamdonaciones.swgdsjucambackend.model.Donacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DonacionRepository extends JpaRepository<Donacion, Long> {
}