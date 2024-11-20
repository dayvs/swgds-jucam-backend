package org.jucamdonaciones.swgdsjucambackend.repository;

import org.jucamdonaciones.swgdsjucambackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
    User findByUsuario_id(Long usuario_id);
}