package io.agentmemoryhub.api.controller;

import io.agentmemoryhub.core.model.record.LoginInputRecord;
import io.agentmemoryhub.core.model.record.LoginResultRecord;
import io.agentmemoryhub.core.model.record.UserRecord;
import io.agentmemoryhub.core.service.auth.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public LoginResultRecord login(@RequestBody LoginInputRecord loginInputRecord) {
        return authenticationService.loginByCredentials(loginInputRecord);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpServletRequest) {
        var accessToken = getBearerToken(httpServletRequest);
        authenticationService.logoutByAccessToken(accessToken);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserRecord getCurrentUser(HttpServletRequest httpServletRequest) {
        var accessToken = getBearerToken(httpServletRequest);
        return authenticationService.findUserByAccessToken(accessToken).orElseThrow(() -> new io.agentmemoryhub.core.exception.HubException("UNAUTHORIZED", "error.unauthorized", 401));
    }

    private String getBearerToken(HttpServletRequest httpServletRequest) {
        var authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

        if (Objects.isNull(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        return authorizationHeader.substring("Bearer ".length()).strip();
    }
}
