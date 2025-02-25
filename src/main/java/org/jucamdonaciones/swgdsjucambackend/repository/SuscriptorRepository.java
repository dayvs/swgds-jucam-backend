package org.jucamdonaciones.swgdsjucambackend.repository;

import org.jucamdonaciones.swgdsjucambackend.model.Suscriptor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuscriptorRepository extends JpaRepository<Suscriptor, Integer> {
    
}