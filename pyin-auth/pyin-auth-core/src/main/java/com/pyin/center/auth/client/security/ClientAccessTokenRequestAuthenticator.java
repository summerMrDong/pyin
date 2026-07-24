package com.pyin.center.auth.client.security;

import com.pyin.center.auth.authentication.AuthenticatedPrincipal;
import com.pyin.center.auth.authentication.ClientRequestAuthenticator;
import com.pyin.center.auth.client.service.ClientAccessTokenService;
import com.pyin.center.auth.client.service.ClientAccessTokenService.ClientAccessToken;
import com.pyin.plugin.system.api.model.SystemClientCredentialIdentity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class ClientAccessTokenRequestAuthenticator implements ClientRequestAuthenticator {

    private final ClientAccessTokenService clientAccessTokenService;

    public ClientAccessTokenRequestAuthenticator(ClientAccessTokenService clientAccessTokenService) {
        this.clientAccessTokenService = clientAccessTokenService;
    }

    @Override
    public AuthenticatedPrincipal authenticate() {
        ClientAccessToken accessToken = clientAccessTokenService.authenticate(currentAuthorizationHeader());
        SystemClientCredentialIdentity credential = accessToken.credential();
        return AuthenticatedPrincipal.clientCredential(credential.id(), credential.accessKey());
    }

    private String currentAuthorizationHeader() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            return request == null ? null : request.getHeader("Authorization");
        }
        return null;
    }
}
