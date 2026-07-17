package com.pyin.gateway.exception;

import com.pyin.gateway.response.PluginGatewayErrorResponseSupport;
import com.pyin.plugin.common.api.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

@Component
public class PluginGatewayExceptionResolver implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        if (!(ex instanceof PluginGatewayException gatewayException)) {
            return null;
        }
        ResponseEntity<byte[]> entity = PluginGatewayErrorResponseSupport.resultResponse(
                gatewayException.getStatusCode(),
                Result.fail(gatewayException.getCode(), gatewayException.getMessage())
        );
        try {
            PluginGatewayErrorResponseSupport.writeResponse(response, entity);
            return new ModelAndView();
        } catch (Exception writeException) {
            return null;
        }
    }
}
