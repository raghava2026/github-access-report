package com.github.accessreport.controller;

import com.github.accessreport.model.AccessReport;
import com.github.accessreport.service.GitHubService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
@RestController
@RequestMapping("/api")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final GitHubService gitHubService;

    // constructor injection instead of @RequiredArgsConstructor
    public ReportController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @GetMapping("/access-report")
    public ResponseEntity<AccessReport> getAccessReport() {
        log.info("Access report endpoint called");
        AccessReport report = gitHubService.buildAccessReport();
        return ResponseEntity.ok(report);
    }

    @GetMapping("/ping")
    public ResponseEntity<Map<String, String>> ping() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        return ResponseEntity.ok(response);
    }
}
