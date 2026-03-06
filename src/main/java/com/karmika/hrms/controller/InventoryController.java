package com.karmika.hrms.controller;

import com.karmika.hrms.entity.InventoryItem;
import com.karmika.hrms.entity.InventoryRequest;
import com.karmika.hrms.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // --- ITEM ENDPOINTS (HR/ADMIN only) ---

    @GetMapping("/items")
    public ResponseEntity<List<InventoryItem>> getAllItems() {
        return ResponseEntity.ok(inventoryService.getAllItems());
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<InventoryItem> createItem(@RequestBody InventoryItem item) {
        return ResponseEntity.ok(inventoryService.createItem(item));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<InventoryItem> updateItem(@PathVariable Long id, @RequestBody InventoryItem item) {
        return ResponseEntity.ok(inventoryService.updateItem(id, item));
    }

    @DeleteMapping("/items/{id}")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN')")
    public ResponseEntity<?> deleteItem(@PathVariable Long id) {
        inventoryService.deleteItem(id);
        return ResponseEntity.ok().build();
    }

    // --- REQUEST ENDPOINTS ---

    @GetMapping("/requests/my-requests")
    public ResponseEntity<List<InventoryRequest>> getMyRequests(Authentication authentication) {
        return ResponseEntity.ok(inventoryService.getMyRequests(authentication.getName()));
    }

    @PostMapping("/requests")
    public ResponseEntity<InventoryRequest> createRequest(Authentication authentication,
            @RequestBody Map<String, Object> payload) {
        Long itemId = Long.valueOf(payload.get("itemId").toString());
        Integer quantity = Integer.valueOf(payload.get("quantity").toString());
        String reason = payload.get("reason") != null ? payload.get("reason").toString() : "";

        return ResponseEntity.ok(inventoryService.createRequest(authentication.getName(), itemId, quantity, reason));
    }

    @GetMapping("/requests/all")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MANAGER')")
    public ResponseEntity<List<InventoryRequest>> getAllRequests() {
        return ResponseEntity.ok(inventoryService.getAllRequests());
    }

    @PutMapping("/requests/{id}/process")
    @PreAuthorize("hasAnyRole('HR', 'ADMIN', 'MANAGER')")
    public ResponseEntity<InventoryRequest> processRequest(
            @PathVariable Long id,
            Authentication authentication,
            @RequestBody Map<String, String> payload) {
        InventoryRequest.RequestStatus action = InventoryRequest.RequestStatus.valueOf(payload.get("action"));
        String comments = payload.get("comments");
        return ResponseEntity.ok(inventoryService.processRequest(id, authentication.getName(), action, comments));
    }
}
