package fr.plb.whatsapp.user.resource;

import fr.plb.whatsapp.user.dto.UserDTO;
import fr.plb.whatsapp.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static fr.plb.whatsapp.configuration.security.AuthoritiesConstants.ADMIN;

@RestController
@RequestMapping("/api/users")
public class UserResource {

    private final UserService userService;

    public UserResource(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('" + ADMIN  + "')")
    public Mono<ResponseEntity<Flux<UserDTO>>> getAll() {
        return Mono.just(ResponseEntity.ok(userService.getAll().map(UserDTO::new)));
    }
}
