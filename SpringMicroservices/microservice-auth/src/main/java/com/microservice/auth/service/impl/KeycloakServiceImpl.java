package com.microservice.auth.service.impl;

import com.microservice.auth.controller.dto.UserDTO;
import com.microservice.auth.service.IKeycloakService;
import com.microservice.auth.util.KeycloakProvider;
import com.microservice.auth.util.Mapper.UserMapper;
import jakarta.ws.rs.core.Response;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class KeycloakServiceImpl implements IKeycloakService {
    @Value("${jwt.auth.converter.token-url}")
    private String TOKEN_URL;
    @Value("${jwt.auth.converter.token-validation}")
    private String TOKEN_VALIDATION;
    @Value("${jwt.auth.converter.resource-id}")
    private String CLIENT_ID;
    @Value("${jwt.auth.converter.client-secret}")
    private String CLIENT_SECRET;

    private final WebClient webClient;

    public KeycloakServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    /*
    *Metodo para listar todos los usuarios de keycloak
    * @return List<UserRepresentation>
     * */
    @Override
    public List<UserRepresentation> finAllUsers() {
        return KeycloakProvider.getRealmResource()
                .users()
                .list();
    }
    /*
     *Metodo para listar todos los usuarios por el username
     * @return List<UserRepresentation>
     * */
    @Override
    public List<UserRepresentation> searchUserByUsername(String username) {
        return KeycloakProvider.getRealmResource()
                .users()
                .searchByUsername(username,true);
    }
    /*
     *Metodo para crear un usuario nuevo en keycloak
     * @return String
     * */
    @Override
    public String createUser(@NonNull UserDTO userDTO) {

       int status=0;
        if (!userDTO.getUsername().matches("^[a-zA-Z0-9._-]{3,}$")) {
            return "Username must be alphanumeric and at least 3 characters.";
        }

        UsersResource usersResource=KeycloakProvider.getUsersResource();

        UserRepresentation userRepresentation=new UserRepresentation();
        userRepresentation.setFirstName(userDTO.getFirstName());
        userRepresentation.setLastName(userDTO.getLastName());
        userRepresentation.setEmail(userDTO.getEmail());
        userRepresentation.setUsername(userDTO.getUsername());
        userRepresentation.setEmailVerified(true);
        userRepresentation.setEnabled(true);

        Response response=usersResource.create(userRepresentation);
        status=response.getStatus();
        if(status == 201){
            String path=response.getLocation().getPath();
            String userId=path.substring(path.lastIndexOf("/")+1);

            CredentialRepresentation credentialRepresentation=new CredentialRepresentation();
            credentialRepresentation.setTemporary(false);
            credentialRepresentation.setType(OAuth2Constants.PASSWORD);
            credentialRepresentation.setValue(userDTO.getPassword());

            usersResource.get(userId).resetPassword(credentialRepresentation);

            RealmResource realmResource=KeycloakProvider.getRealmResource();

            List<RoleRepresentation> roleRepresentations=null;

            if(userDTO.getRoles() ==null || userDTO.getRoles().isEmpty()){
                roleRepresentations= List.of(realmResource.roles().get("user").toRepresentation());
            }else{
                roleRepresentations= realmResource.roles()
                        .list()
                        .stream()
                        .filter(role -> userDTO.getRoles()
                                .stream()
                                .anyMatch( rolename -> rolename.equalsIgnoreCase(role.getName())))
                        .toList();
            }

            realmResource.users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .add(roleRepresentations);
            return "user created successfully!!";


        } else if (status ==409) {
            log.error("User exist already!!");
            return  "User exist already!!";
        }else{
            log.error("Error creating user, please contact with the administrator");
            return "Error creating user, please contact with the administrator";
        }
    }
    /*
     *Metodo para eliminar un usuario  en keycloak
     * @return Void
     * */
    @Override
    public void deleteUser(String userId) {
        KeycloakProvider.getUsersResource()
                .get(userId)
                .remove();
    }
    /*
     *Metodo para actualizar un usuario  en keycloak
     * @return Void
     * */
    @Override
    public void updateUser(String userId, UserDTO userDTO) {
        CredentialRepresentation credentialRepresentation=new CredentialRepresentation();
        credentialRepresentation.setTemporary(false);
        credentialRepresentation.setType(OAuth2Constants.PASSWORD);
        credentialRepresentation.setValue(userDTO.getPassword());

        UserRepresentation userRepresentation=new UserRepresentation();
        userRepresentation.setFirstName(userDTO.getFirstName());
        userRepresentation.setLastName(userDTO.getLastName());
        userRepresentation.setEmail(userDTO.getEmail());
        userRepresentation.setUsername(userDTO.getUsername());
        userRepresentation.setEmailVerified(true);
        userRepresentation.setEnabled(true);
        userRepresentation.setCredentials(Collections.singletonList(credentialRepresentation));

        UserResource userResource=KeycloakProvider.getUsersResource().get(userId);
        userResource.update(userRepresentation);

    }

    @Override
    public Map<String, Object> login(String username, String password) {
        try {
            return webClient.post()
                    .uri(TOKEN_URL)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData("grant_type", "password")
                            .with("client_id", CLIENT_ID)
                            .with("client_secret", CLIENT_SECRET)
                            .with("username", username)
                            .with("password", password))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Login error: {}", e.getMessage());
            throw new RuntimeException("Credenciales incorrectas");
        }
    }

    @Override
    public Map<String, Object> refreshToken(String refreshToken) {
        return webClient.post()
                .uri(TOKEN_URL)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("client_id", CLIENT_ID)
                        .with("client_secret", CLIENT_SECRET)
                        .with("refresh_token", refreshToken))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    @Override
    public Map<String, Object> validateToken(String token) {
        return webClient.post()
                .uri(TOKEN_VALIDATION)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters
                        .fromFormData("token", token)
                        .with("client_id", CLIENT_ID)
                        .with("client_secret", CLIENT_SECRET))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
    }

    @Override
    public UserDTO getUserById(String userId) {
        UserRepresentation user = KeycloakProvider.getUsersResource().get(userId).toRepresentation();

        List<RoleRepresentation> roleList = KeycloakProvider
                .getRealmResource()
                .users()
                .get(userId)
                .roles()
                .realmLevel()
                .listEffective();

        return UserMapper.toDto(user, roleList);
    }

    @Override
    public void assignRoles(String userId, List<String> roles) {
        RealmResource realm = KeycloakProvider.getRealmResource();
        List<RoleRepresentation> roleRepresentations = roles.stream()
                .map(roleName -> realm.roles().get(roleName).toRepresentation())
                .toList();

        realm.users().get(userId).roles().realmLevel().add(roleRepresentations);
    }

    @Override
    public void setUserEnabled(String userId, boolean enabled) {
        UserResource userResource = KeycloakProvider.getUsersResource().get(userId);
        UserRepresentation user = userResource.toRepresentation();
        user.setEnabled(enabled);
        userResource.update(user);
    }
}
