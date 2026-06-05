package com.lawapp.backend.security;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;

/**
 * WebSocket el sıkışma aşamasında JWT doğrulaması yapan interceptor.
 * Token, Sec-WebSocket-Protocol header'ından "bearer.{jwt}" formatında alınır.
 * Bu sayede JWT asla URL query parametresinde yer almaz ve loglanmaz.
 */
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);
    private static final String BEARER_PREFIX = "bearer.";

    private final JwtUtils jwtUtils;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        String token = extractTokenFromSubprotocol(request);

        if (token == null) {
            // Geriye dönük uyumluluk: query parametresinden de kontrol et (geçiş dönemi)
            token = extractTokenFromQuery(request);
        }

        if (token != null && jwtUtils.validateJwtToken(token)) {
            String email = jwtUtils.getUserNameFromJwtToken(token);
            attributes.put("email", email);
            attributes.put("authenticated", true);

            // Subprotocol negotiation: İstemciye aynı subprotocol'ü geri döndür
            response.getHeaders().put("Sec-WebSocket-Protocol",
                    List.of(BEARER_PREFIX + token));

            logger.info("WebSocket handshake successful for user: {}", maskEmail(email));
            return true;
        }

        logger.warn("WebSocket handshake rejected: Invalid or missing JWT token");
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // El sıkışma sonrası ek işlem gerekmez
    }

    /**
     * Sec-WebSocket-Protocol header'ından "bearer.{jwt}" formatındaki token'ı çıkarır.
     */
    private String extractTokenFromSubprotocol(ServerHttpRequest request) {
        List<String> protocols = request.getHeaders().get("Sec-WebSocket-Protocol");
        if (protocols != null) {
            for (String protocol : protocols) {
                // Virgülle ayrılmış subprotocol listesi olabilir
                String[] parts = protocol.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.toLowerCase().startsWith(BEARER_PREFIX)) {
                        return trimmed.substring(BEARER_PREFIX.length());
                    }
                }
            }
        }
        return null;
    }

    /**
     * Geriye dönük uyumluluk: URL query parametresinden token'ı çıkarır.
     * Bu yöntem geçiş döneminde kullanılır ve ileride kaldırılmalıdır.
     */
    private String extractTokenFromQuery(ServerHttpRequest request) {
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    logger.warn("JWT token detected in URL query parameter. " +
                            "This method is deprecated and will be removed. " +
                            "Please migrate to Sec-WebSocket-Protocol header.");
                    return param.substring(6);
                }
            }
        }
        return null;
    }

    /**
     * E-posta adresini log'larda maskeleyerek gösterir.
     * Örnek: "caner@example.com" → "ca***@example.com"
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        String masked = local.length() <= 2
                ? local + "***"
                : local.substring(0, 2) + "***";
        return masked + "@" + parts[1];
    }
}
