package org.jucamdonaciones.swgdsjucambackend.controller;

import org.jucamdonaciones.swgdsjucambackend.model.User;
import org.jucamdonaciones.swgdsjucambackend.model.Role;
import org.jucamdonaciones.swgdsjucambackend.repository.UserRepository;
import org.jucamdonaciones.swgdsjucambackend.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.transaction.Transactional;


@RestController
@RequestMapping("/usuarios")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. GET /usuarios - Obtener la lista de usuarios
    @GetMapping
    @Transactional
    public ResponseEntity<?> obtenerUsuarios() {
        List<User> usuarios = userRepository.findAll();
        List<Object> response = usuarios.stream().map(usuario -> new Object() {
            public Long usuarioId = usuario.getUsuarioId();
            public String nombre = usuario.getNombre();
            public String apellidos = usuario.getApellidos();
            public String email = usuario.getEmail();
            public String rol = usuario.getRol().getNombre();
            public String estado = usuario.getEstado();
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 2. GET /usuarios/{usuarioId} - Obtener un usuario por ID
    @Transactional
    @GetMapping("/{usuarioId}")    
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Long usuarioId) {
        User usuario = userRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }
        Object response = new Object() {
            public Long usuarioId = usuario.getUsuarioId();
            public String nombre = usuario.getNombre();
            public String apellidos = usuario.getApellidos();
            public String email = usuario.getEmail();
            public String rol = usuario.getRol().getNombre();
            public String estado = usuario.getEstado();
        };
        return ResponseEntity.ok(response);
    }

    // 3. PUT /usuarios/{usuarioId} - Actualizar un usuario
    @PutMapping("/{usuarioId}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long usuarioId, @RequestBody User updatedUser) {
        User existingUser = userRepository.findById(usuarioId).orElse(null);
        if (existingUser == null) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }

        boolean hasChanges = false;

        if (updatedUser.getNombre() != null && !existingUser.getNombre().equals(updatedUser.getNombre())) {
            existingUser.setNombre(updatedUser.getNombre());
            hasChanges = true;
        }
        if (updatedUser.getApellidos() != null && !existingUser.getApellidos().equals(updatedUser.getApellidos())) {
            existingUser.setApellidos(updatedUser.getApellidos());
            hasChanges = true;
        }
        if (updatedUser.getEmail() != null && !existingUser.getEmail().equals(updatedUser.getEmail())) {
            existingUser.setEmail(updatedUser.getEmail());
            hasChanges = true;
        }
        if (updatedUser.getRol() != null && !existingUser.getRol().getNombre().equals(updatedUser.getRol().getNombre())) {
            Role newRole = roleRepository.findByNombre(updatedUser.getRol().getNombre());
            if (newRole == null) {
                return ResponseEntity.badRequest().body("Ingresa un rol válido del tipo string. Únicamente puede ser 'Administrador' o 'Empleado'");
            }
            existingUser.setRol(newRole);
            hasChanges = true;
        }

        if (hasChanges) {
            userRepository.save(existingUser);
            return ResponseEntity.ok("Usuario actualizado con éxito");
        } else {
            return ResponseEntity.ok("No hay cambios para actualizar");
        }
    }
    
    // 4. POST /usuarios/{usuarioId}/reset-password - Restablecer contraseña
    @PostMapping("/{usuarioId}/reset-password")
    public ResponseEntity<?> resetPasswordByAdmin(@PathVariable Long usuarioId) {
        User user = userRepository.findById(usuarioId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }
        user.setPassword(passwordEncoder.encode("Password1"));
        user.setRequiere_cambio_contraseña(true);
        userRepository.save(user);

        return ResponseEntity.ok("Contraseña restablecida exitosamente");
    }



    // 5. DELETE /usuarios/{usuarioId} - Eliminar un usuario
    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long usuarioId) {
        User usuario = userRepository.findById(usuarioId).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }
        userRepository.delete(usuario);
        return ResponseEntity.ok("Usuario eliminado con éxito");
    }

    // 6. POST /usuarios - Crear un nuevo usuario
    @PostMapping
    public ResponseEntity<?> crearUsuario(@RequestBody User newUser) {
        // Validaciones
        if (newUser.getNombre() == null || newUser.getNombre().length() < 3) {
            return ResponseEntity.badRequest().body(new ErrorResponse("petición incorrecta", "Ingresa un nombre válido del tipo string con al menos 3 caracteres"));
        }
        if (newUser.getApellidos() == null || newUser.getApellidos().length() < 3) {
            return ResponseEntity.badRequest().body(new ErrorResponse("petición incorrecta", "Ingresa un apellido válido del tipo string con al menos 3 caracteres"));
        }
        if (newUser.getEmail() == null || !newUser.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body(new ErrorResponse("petición incorrecta", "Ingresa un email válido del tipo string"));
        }
        if (newUser.getRol() == null || (!newUser.getRol().getNombre().equals("Administrador") && !newUser.getRol().getNombre().equals("Empleado"))) {
            return ResponseEntity.badRequest().body(new ErrorResponse("petición incorrecta", "Ingresa un rol válido del tipo string. Únicamente puede ser 'Administrador' o 'Empleado'"));
        }

        // Verificar si el correo ya existe
        if (userRepository.findByEmail(newUser.getEmail()) != null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("petición incorrecta", "El correo electrónico ya está en uso"));
        }

        // Asignar el rol
        Role role = roleRepository.findByNombre(newUser.getRol().getNombre());
        if (role == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("petición incorrecta", "Rol inválido"));
        }
        newUser.setRol(role);

        // Establecer estado y contraseña por defecto
        newUser.setEstado("activo");
        newUser.setPassword(passwordEncoder.encode("Password1"));
        newUser.setRequiere_cambio_contraseña(true);

        userRepository.save(newUser);

        // Construir la respuesta
        Object response = new Object() {
            public Long usuarioId = newUser.getUsuarioId();
            public String nombre = newUser.getNombre();
            public String apellidos = newUser.getApellidos();
            public String email = newUser.getEmail();
            public String rol = newUser.getRol().getNombre();
            public String estado = newUser.getEstado();
            public String fecha_creacion = LocalDateTime.now().toString();
            public String mensaje = "Empleado guardado con éxito";
        };

        return ResponseEntity.status(201).body(response);
    }

    // Clase para manejar errores
    static class ErrorResponse {
        public String error;
        public String mensaje;

        public ErrorResponse(String error, String mensaje) {
            this.error = error;
            this.mensaje = mensaje;
        }
    }

    @PutMapping("/cambiar-password")
    public ResponseEntity<?> cambiarPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");
        String confirmPassword = payload.get("confirmPassword");
        
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }
        
        // Validar que la contraseña antigua coincida
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body("La contraseña antigua no es correcta");
        }
        
        // Validar que la nueva contraseña sea diferente a la antigua
        if (oldPassword.equals(newPassword)) {
            return ResponseEntity.badRequest().body("La nueva contraseña no puede ser la misma que la contraseña anterior");
        }
        
        // Validar que la nueva contraseña y su confirmación coincidan
        if (!newPassword.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body("La contraseña no coincide");
        }
        
        try {
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setRequiere_cambio_contraseña(false);
            userRepository.save(user);
            return ResponseEntity.ok("Contraseña actualizada correctamente");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("No se pudo guardar tu nueva contraseña, intenta de nuevo más tarde");
        }
    }

}