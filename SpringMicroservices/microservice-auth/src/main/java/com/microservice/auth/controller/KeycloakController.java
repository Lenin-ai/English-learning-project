package com.microservice.auth.controller;

import com.microservice.auth.controller.dto.UserDTO;
import com.microservice.auth.service.IKeycloakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/keycloak")
public class KeycloakController {

    @Autowired
    private IKeycloakService iKeycloakService;

    /*
    * CAPA USUARIOS
    *
    * */
    //Obtener datos de tu perfil
    @GetMapping("/me")
    public ResponseEntity<?> getProfile() {
        String userId = getUserIdFromToken();
        return ResponseEntity.ok(iKeycloakService.getUserById(userId));
    }
    //Eliminar un usuario completo /cuenta
    @DeleteMapping("/delete")
    public ResponseEntity<?>  deleteUser() {
        String userId = getUserIdFromToken();
        iKeycloakService.deleteUser(userId);
        return  ResponseEntity.noContent().build();
    }
    //Iniciar seccion y retornar un token user-admin
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginRequest) {
        try {
            Map<String, Object> token = iKeycloakService.login(
                    loginRequest.get("username"),
                    loginRequest.get("password")
            );
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciales incorrectas");
        }
    }
    //Registrarse a la app  admin - User
    @PostMapping("/create")
    public ResponseEntity<?>  createUser(@RequestBody UserDTO userDTO) throws URISyntaxException {
        String response= iKeycloakService.createUser(userDTO);
        return  ResponseEntity.created(new URI("/keycloak/user/create")).body(response);
    }
    //Actualizar los datos de un usuario - user
    @PutMapping("/update")
    public ResponseEntity<?>  updateUser(@RequestBody UserDTO userDTO) {
        String userId = getUserIdFromToken();
        iKeycloakService.updateUser(userId,userDTO);
        return  ResponseEntity.ok("User Update Successfully!");
    }
    //Refrescar token
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refresh_token");
        return ResponseEntity.ok(iKeycloakService.refreshToken(refreshToken));
    }
    //Activar o descartivar un usuario por id
    @PutMapping("/enabled")
    public ResponseEntity<?> setUserEnabled( @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", false);
        String userId = getUserIdFromToken();
        iKeycloakService.setUserEnabled(userId, enabled);
        return ResponseEntity.ok("Estado actualizado correctamente");
    }
    /*
     * CAPA ADMINISTRADOR
     *
     * */
    //Listar todos los usuarios -admin
    @GetMapping("/search")
    @PreAuthorize("hasRole('admin_client_role')")
    public ResponseEntity<?> findAllUsers(){
        return  ResponseEntity.ok(iKeycloakService.finAllUsers());
    }
    //buscar un usuario por su username - admin
    @GetMapping("/search/{username}")
    @PreAuthorize("hasRole('admin_client_role')")
    public ResponseEntity<?> findUserByUsername(@PathVariable String username){
        return  ResponseEntity.ok(iKeycloakService.searchUserByUsername(username));
    }

    //Asignar roles  a un usuario  -admin
    @PostMapping("/assign-roles")
    @PreAuthorize("hasRole('admin_client_role')")
    public ResponseEntity<?> assignRoles( @RequestBody List<String> roles) {
        String userId = getUserIdFromToken();
        iKeycloakService.assignRoles(userId, roles);
        return ResponseEntity.ok("Roles asignados correctamente");
    }
    //Habilitar a un usuario si desactiuvo su cuenta
    @PutMapping("/admin/enabled")
    public ResponseEntity<?> setUserEnabledByAdmin(@RequestBody Map<String, Object> body) {
        String userId = (String) body.get("userId");
        boolean enabled = (Boolean) body.getOrDefault("enabled", false);

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body("userId es requerido");
        }

        iKeycloakService.setUserEnabled(userId, enabled);
        return ResponseEntity.ok("Usuario " + (enabled ? "activado" : "desactivado") + " correctamente");
    }


    private String getUserIdFromToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return jwt.getSubject();
    }
}
