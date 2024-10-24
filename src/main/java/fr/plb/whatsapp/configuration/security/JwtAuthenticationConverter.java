package fr.plb.whatsapp.configuration.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fr.plb.whatsapp.configuration.security.SecurityUtils.AUTHORITIES_KEY;

@Component
public class JwtAuthenticationConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt token) {
        List<SimpleGrantedAuthority> grantedAuthorities = new ArrayList<>();
        if (token.getClaim(AUTHORITIES_KEY) instanceof String authoritiesRaw) {
            grantedAuthorities = Arrays.stream(authoritiesRaw.split(" "))
                    .map(SimpleGrantedAuthority::new)
                    .toList();
        }
        return Mono.just(new JwtAuthenticationToken(token, grantedAuthorities));
    }
}
