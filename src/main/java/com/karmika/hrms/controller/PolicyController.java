package com.karmika.hrms.controller;

import com.karmika.hrms.entity.OrganizationPolicy;
import com.karmika.hrms.repository.OrganizationPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final OrganizationPolicyRepository policyRepository;

    @GetMapping
    public ResponseEntity<List<OrganizationPolicy>> getAllPolicies() {
        return ResponseEntity.ok(policyRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<?> createPolicy(@RequestBody OrganizationPolicy policy) {
        return ResponseEntity.ok(policyRepository.save(policy));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<?> updatePolicy(@PathVariable Long id, @RequestBody OrganizationPolicy policyDetails) {
        return policyRepository.findById(id)
                .map(policy -> {
                    policy.setTitle(policyDetails.getTitle());
                    policy.setDescription(policyDetails.getDescription());
                    policy.setContent(policyDetails.getContent());
                    policy.setDocumentUrl(policyDetails.getDocumentUrl());
                    policy.setVersion(policyDetails.getVersion());
                    return ResponseEntity.ok(policyRepository.save(policy));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<?> deletePolicy(@PathVariable Long id) {
        return policyRepository.findById(id)
                .map(policy -> {
                    policyRepository.delete(policy);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Policy deleted successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
