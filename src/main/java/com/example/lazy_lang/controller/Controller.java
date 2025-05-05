package com.example.lazy_lang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.lazy_lang.service.ServiceBackend;

@RestController
@RequestMapping("/api")
public class Controller {
    @Autowired
    private ServiceBackend serviceBackend;

    @RequestMapping("/getAccessToken")
    /**
     * This method is used to get the access token from the server.
     * @return access token
     */
    public Object getAccessToken() throws Exception{
        return serviceBackend.getAccessToken();
    }
}
