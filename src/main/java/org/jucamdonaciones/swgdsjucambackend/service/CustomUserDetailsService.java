package org.jucamdonaciones.swgdsjucambackend.service;

import org.jucamdonaciones.swgdsjucambackend.model.User;
import org.jucamdonaciones.swgdsjucambackend.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User usuario = userRepository.findByEmail(email);
        if (usuario == null) {
            throw new UsernameNotFoundException("Usuario no encontrado");
        }
        return new org.springframework.security.core.userdetails.User(
                usuario.getEmail(),
                usuario.getPassword(),
                true,
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority(usuario.getRol().getNombre()))                
        );
    }
}
