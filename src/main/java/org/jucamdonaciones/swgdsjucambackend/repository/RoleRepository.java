package org.jucamdonaciones.swgdsjucambackend.repository;

import org.jucamdonaciones.swgdsjucambackend.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByNombre(String nombre);
}