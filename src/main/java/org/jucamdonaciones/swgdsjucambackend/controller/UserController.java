package org.jucamdonaciones.swgdsjucambackend.controller;

import org.jucamdonaciones.swgdsjucambackend.model.User;
import org.jucamdonaciones.swgdsjucambackend.model.Role;
import org.jucamdonaciones.swgdsjucambackend.repository.UserRepository;
import org.jucamdonaciones.swgdsjucambackend.repository.RoleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/usuarios")
public class UserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. GET /usuarios - Obtener la lista de usuarios
    @GetMapping
    public ResponseEntity<?> obtenerUsuarios() {
        List<User> usuarios = userRepository.findAll();
        List<Object> response = usuarios.stream().map(usuario -> {
            return new Object() {
                public Long usuario_id = usuario.getUsuario_id();
                public String nombre = usuario.getNombre();
                public String apellidos = usuario.getApellidos();
                public String email = usuario.getEmail();
                public String rol = usuario.getRol().getNombre();
                public String estado = usuario.getEstado();
            };
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 2. GET /usuarios/{usuario_id} - Obtener un usuario por ID
    @GetMapping("/{usuario_id}")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Long usuario_id) {
        User usuario = userRepository.findById(usuario_id).orElse(null);
        if (usuario == null) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }
        Object response = new Object() {
            public Long usuario_id = usuario.getUsuario_id();
            public String nombre = usuario.getNombre();
            public String apellidos = usuario.getApellidos();
            public String email = usuario.getEmail();
            public String rol = usuario.getRol().getNombre();
            public String estado = usuario.getEstado();
        };
        return ResponseEntity.ok(response);
    }

    // 3. PUT /usuarios/{usuario_id} - Actualizar un usuario
    @PutMapping("/{usuario_id}")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Long usuario_id, @RequestBody User updatedUser) {
        User existingUser = userRepository.findById(usuario_id).orElse(null);
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
    
    // 4. POST /usuarios/{usuario_id}/reset-password - Restablecer contraseña
    @PostMapping("/{usuario_id}/reset-password")
    public ResponseEntity<?> resetPasswordByAdmin(@PathVariable Long usuario_id) {
        User user = userRepository.findById(usuario_id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("Usuario no encontrado");
        }
        user.setPassword(passwordEncoder.encode("Password1"));
        user.setRequiere_cambio_contraseña(true);
        userRepository.save(user);

        return ResponseEntity.ok("Contraseña restablecida exitosamente");
    }



    // 5. DELETE /usuarios/{usuario_id} - Eliminar un usuario
    @DeleteMapping("/{usuario_id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Long usuario_id) {
        User usuario = userRepository.findById(usuario_id).orElse(null);
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
            public Long usuario_id = newUser.getUsuario_id();
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
}