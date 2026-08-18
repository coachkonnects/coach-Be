package com.coachkonnects.backend.config;

import com.coachkonnects.backend.service.ModerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Type;
import java.util.Map;

@ControllerAdvice
public class GlobalModerationAdvice extends RequestBodyAdviceAdapter {

    @Autowired
    private ModerationService moderationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private HttpServletRequest request;

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        String uri = request.getRequestURI();
        if (uri != null && uri.startsWith("/api/admin")) {
            return body;
        }

        try {
            String json = objectMapper.writeValueAsString(body);
            moderationService.validateContent(json);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Ignore serialization errors
        }
        return body;
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        if (ex.getMessage() != null && ex.getMessage().startsWith("CONTENT_BLOCKED")) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
        throw ex;
    }
}
