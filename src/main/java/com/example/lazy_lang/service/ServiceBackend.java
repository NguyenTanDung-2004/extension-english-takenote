package com.example.lazy_lang.service;

import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.json.JSONObject;
import java.util.Collections;
import java.util.Map;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


@Service
public class ServiceBackend {

    @Autowired
    private JWTService jwtService;

    public Object getAccessToken() throws Exception{
        String token = jwtService.generateJWT();
        RestTemplate restTemplate = new RestTemplate();
    String tokenUrl = "https://oauth2.googleapis.com/token";

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    map.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
    map.add("assertion", token); // chính là signedJWT.serialize()

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

    try {
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
        return  response;
    } catch (HttpClientErrorException e) {
        System.err.println("Lỗi khi gọi Google: " + e.getResponseBodyAsString());
        throw e;
    }
    }
}
