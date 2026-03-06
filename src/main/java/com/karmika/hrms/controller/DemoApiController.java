package com.karmika.hrms.controller;

import com.karmika.hrms.service.DemoApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
@RequiredArgsConstructor
public class DemoApiController {

    private final DemoApiService demoApiService;

    @GetMapping("/users")
    public ResponseEntity<String> getRandomUsers(@RequestParam(defaultValue = "6") int results) {
        String responseBody = demoApiService.getRandomUsers(results);
        return ResponseEntity.ok(responseBody);
    }
}
