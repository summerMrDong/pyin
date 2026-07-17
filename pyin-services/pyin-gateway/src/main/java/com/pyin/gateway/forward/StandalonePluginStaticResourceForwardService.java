package com.pyin.gateway.forward;

import com.pyin.gateway.path.PluginGatewayPathSupport;
import com.pyin.gateway.path.PluginStaticResourcePathSupport;
import com.pyin.plugin.runtime.registry.RegisteredPlugin;
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
import org.springframework.web.client.RestTemplate;

@Service
public class StandalonePluginStaticResourceForwardService {

    private final RestTemplate restTemplate;

    public StandalonePluginStaticResourceForwardService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void forward(RegisteredPlugin plugin, HttpServletRequest request, HttpServletResponse response) throws IOException {
        byte[] body = request.getInputStream().readAllBytes();
        HttpHeaders headers = extractHeaders(request);
        ResponseEntity<byte[]> forwardResponse = restTemplate.exchange(
                buildTargetUrl(plugin.frontendBaseUrl(), request),
                HttpMethod.valueOf(request.getMethod()),
                new HttpEntity<>(body, headers),
                byte[].class
        );
        writeResponse(response, forwardResponse);
    }

    private String buildTargetUrl(String baseUrl, HttpServletRequest request) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String requestPath = PluginGatewayPathSupport.requestPath(request);
        String relativePath = PluginStaticResourcePathSupport.extractRelativeAssetPath(requestPath);
        String queryString = request.getQueryString();
        return normalizedBaseUrl + "/" + relativePath + (queryString == null || queryString.isBlank() ? "" : "?" + queryString);
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
        filterResponseHeaders(forwardResponse.getHeaders())
                .forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
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
