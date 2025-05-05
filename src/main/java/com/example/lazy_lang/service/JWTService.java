package com.example.lazy_lang.service;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import java.util.Base64;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Component
public class JWTService {
    
    @Value("${json.private_key}")
    private String privateKey;

    @Value("${json.aud}")
    private String aud;

    @Value("${json.client_email}")
    private String iss;

    @Value("${json.scope}")
    private String scope;


    public String generateJWT() {
        System.out.println(privateKey);
        try {
            // 1. Tạo JWS Header
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            // 2. Tạo JWT claims
            JWTClaimsSet claim = new JWTClaimsSet.Builder()
                    .issuer(iss)
                    .audience(aud)
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
                    .claim("scope", scope)
                    .build();

            // 3. Convert PEM private key sang RSAPrivateKey
            String privateKeyPem = privateKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\n", "")             // Nếu chuỗi đọc từ file json có \\n
                .replace("\n", "")              // Nếu chuỗi đã có newline thực
                .replaceAll("\\s+", "");   
                
            System.out.println(privateKeyPem);

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyPem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) kf.generatePrivate(spec);

            // 4. Tạo JWT và ký
            SignedJWT signedJWT = new SignedJWT(header, claim);
            signedJWT.sign(new RSASSASigner(rsaPrivateKey));

            // 5. Trả JWT
            return signedJWT.serialize();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to generate JWT");
        }
    }


    public Map<String, Object> decodeJWT(String token) {
        Map<String, Object> claimsMap = new HashMap<>();
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(privateKey.getBytes());

            if (signedJWT.verify(verifier)) {
                JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                claimsMap.put("subject", claims.getSubject());
                claimsMap.put("issuer", claims.getIssuer());
                claimsMap.put("issueTime", claims.getIssueTime());
                claimsMap.put("expirationTime", claims.getExpirationTime());
                claimsMap.put("roleId", claims.getClaim("roleId"));
                claimsMap.put("userId", claims.getClaim("userId"));
            } else {
                throw new RuntimeException("JWT verification failed");
            }
        } catch (Exception e) {
            // e.printStackTrace();
            throw new RuntimeException("Failed to decode JWT");
        }
        return claimsMap;
    }
}
