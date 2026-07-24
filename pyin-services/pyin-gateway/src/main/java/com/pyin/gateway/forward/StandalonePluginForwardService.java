package com.pyin.gateway.forward;

import com.pyin.center.auth.authentication.AuthenticatedPrincipal;
import com.pyin.gateway.exception.PluginGatewayExceptionFactory;
import com.pyin.gateway.path.PluginGatewayPathSupport;
import com.pyin.gateway.path.PluginGatewayPathSupport.PluginGatewayPath;
import com.pyin.gateway.signature.GatewaySignatureService;
import com.pyin.plugin.runtime.route.PluginApiRoute;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class StandalonePluginForwardService {

    private final GatewaySignatureService gatewaySignatureService;
    private final RestTemplate restTemplate;

    public StandalonePluginForwardService(
            GatewaySignatureService gatewaySignatureService,
            RestTemplate restTemplate
    ) {
        this.gatewaySignatureService = gatewaySignatureService;
        this.restTemplate = restTemplate;
    }

    public void forward(
            PluginApiRoute route,
            PluginGatewayPath path,
            AuthenticatedPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        byte[] body = request.getInputStream().readAllBytes();
        HttpHeaders headers = extractHeaders(request);
        String targetPath = PluginGatewayPathSupport.toPluginControllerPath(path);
        headers.setAll(gatewaySignatureService.buildForwardHeaders(
                route,
                path,
                principal,
                request.getMethod(),
                targetPath,
                body
        ));

        ResponseEntity<byte[]> forwardResponse = exchange(
                route.pluginId(),
                buildTargetUrl(route.pluginId(), route.backendBaseUrl(), targetPath, request),
                request,
                body,
                headers
        );
        writeResponse(response, forwardResponse);
    }

    private ResponseEntity<byte[]> exchange(
            String pluginId,
            String targetUrl,
            HttpServletRequest request,
            byte[] body,
            HttpHeaders headers
    ) {
        try {
            return restTemplate.exchange(
                targetUrl,
                HttpMethod.valueOf(request.getMethod()),
                new HttpEntity<>(body, headers),
                byte[].class
            );
        } catch (ResourceAccessException exception) {
            throw PluginGatewayExceptionFactory.gatewayTimeout(pluginId, exception.getMessage());
        } catch (RestClientException exception) {
            throw PluginGatewayExceptionFactory.badGateway(pluginId, exception.getMessage());
        }
    }

    private String buildTargetUrl(String pluginId, String baseUrl, String targetPath, HttpServletRequest request) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw PluginGatewayExceptionFactory.badGateway(pluginId, "独立插件后端地址为空");
        }
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String queryString = request.getQueryString();
        return normalizedBaseUrl + targetPath + (queryString == null || queryString.isBlank() ? "" : "?" + queryString);
    }

    private HttpHeaders extractHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Collections.list(request.getHeaderNames()).forEach(name -> {
            if ("host".equalsIgnoreCase(name) || "content-length".equalsIgnoreCase(name)) {
                return;
            }
            headers.put(name, Collections.list(request.getHeaders(name)));
        });
        return headers;
    }

    private void writeResponse(HttpServletResponse response, ResponseEntity<byte[]> forwardResponse) throws IOException {
        response.setStatus(forwardResponse.getStatusCode().value());
        filterResponseHeaders(forwardResponse.getHeaders()).forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        byte[] responseBody = forwardResponse.getBody();
        if (responseBody != null && responseBody.length > 0) {
            response.getOutputStream().write(responseBody);
        }
    }

    private HttpHeaders filterResponseHeaders(HttpHeaders source) {
        HttpHeaders target = new HttpHeaders();
        for (String headerName : source.keySet()) {
            if (isHopByHopHeader(headerName)) {
                continue;
            }
            List<String> values = source.get(headerName);
            if (values != null) {
                target.put(headerName, values);
            }
        }
        return target;
    }

    private boolean isHopByHopHeader(String headerName) {
        return "transfer-encoding".equalsIgnoreCase(headerName)
                || "content-length".equalsIgnoreCase(headerName)
                || "connection".equalsIgnoreCase(headerName)
                || "keep-alive".equalsIgnoreCase(headerName)
                || "proxy-connection".equalsIgnoreCase(headerName)
                || "upgrade".equalsIgnoreCase(headerName);
    }
}
