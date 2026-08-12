package com.order.orderprocessing.service;

import com.order.orderprocessing.dto.CurrentUser;
import com.order.orderprocessing.entity.AppUser;
import com.order.orderprocessing.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class JwtService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();
    private static final String HEADER = ENCODER.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
            .getBytes(StandardCharsets.UTF_8));

    private final byte[] secret;
    private final long expirationSeconds;
    private final ObjectMapper objectMapper;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration}") long expirationSeconds,
                      ObjectMapper objectMapper) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
        this.objectMapper = objectMapper;
    }

    public String createToken(AppUser user) {
        try {
            JwtPayload claims = new JwtPayload(user.getId(), user.getEmail(), user.getRole(),
                    Instant.now().plusSeconds(expirationSeconds).getEpochSecond());
            String payload = ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            String unsigned = HEADER + "." + payload;
            return unsigned + "." + sign(unsigned);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot create access token", exception);
        }
    }

    public CurrentUser parse(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw new IllegalArgumentException("Invalid token");
            String unsigned = parts[0] + "." + parts[1];
            if (!MessageDigest.isEqual(sign(unsigned).getBytes(StandardCharsets.UTF_8),
                    parts[2].getBytes(StandardCharsets.UTF_8))) {
                throw new IllegalArgumentException("Invalid signature");
            }
            JwtPayload payload = objectMapper.readValue(DECODER.decode(parts[1]), JwtPayload.class);
            if (payload.exp() <= Instant.now().getEpochSecond()) throw new IllegalArgumentException("Token expired");
            return new CurrentUser(payload.sub(), payload.email(), payload.role());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid access token", exception);
        }
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private record JwtPayload(Long sub, String email, Role role, long exp) {}
}
