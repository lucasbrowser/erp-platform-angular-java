package com.erp.core.platform.identity;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform")
public class CurrentIdentityController {

    @GetMapping("/me")
    public CurrentIdentityResponse currentIdentity(JwtAuthenticationToken authentication) {
        String subject = authentication.getToken().getSubject();
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        return new CurrentIdentityResponse(subject, authorities);
    }
}
