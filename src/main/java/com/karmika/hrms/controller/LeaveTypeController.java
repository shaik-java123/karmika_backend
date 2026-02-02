package com.karmika.hrms.controller;

import com.karmika.hrms.entity.LeaveType;
import com.karmika.hrms.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave-types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeRepository leaveTypeRepository;

    @GetMapping
    public ResponseEntity<List<LeaveType>> getAllLeaveTypes() {
        return ResponseEntity.ok(leaveTypeRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<?> createLeaveType(@RequestBody LeaveType leaveType) {
        if (leaveTypeRepository.findByCode(leaveType.getCode()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Leave type code already exists"));
        }
        return ResponseEntity.ok(leaveTypeRepository.save(leaveType));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<?> updateLeaveType(@PathVariable Long id, @RequestBody LeaveType details) {
        return leaveTypeRepository.findById(id)
                .map(type -> {
                    try {
                        type.setName(details.getName());
                        type.setDescription(details.getDescription());
                        type.setActive(details.getActive());
                        type.setDefaultDaysPerYear(details.getDefaultDaysPerYear());
                        return ResponseEntity.ok(leaveTypeRepository.save(type));
                    } catch (Exception e) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Error saving leave type: " + e.getMessage()));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<?> deleteLeaveType(@PathVariable Long id) {
        // Only soft delete or check usage before delete
        // For now simple delete, usually blocked if used in foreign keys
        try {
            leaveTypeRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete leave type in use"));
        }
    }
}
