package org.jucamdonaciones.swgdsjucambackend.repository;

import org.jucamdonaciones.swgdsjucambackend.model.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuscripcionRepository extends JpaRepository<Suscripcion, Integer> {
    Suscripcion findByClipSubscriptionId(String clipSubscriptionId);
}