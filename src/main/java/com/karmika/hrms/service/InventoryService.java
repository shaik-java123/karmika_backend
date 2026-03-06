package com.karmika.hrms.service;

import com.karmika.hrms.entity.Employee;
import com.karmika.hrms.entity.InventoryItem;
import com.karmika.hrms.entity.InventoryRequest;
import com.karmika.hrms.repository.EmployeeRepository;
import com.karmika.hrms.repository.InventoryItemRepository;
import com.karmika.hrms.repository.InventoryRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryRequestRepository inventoryRequestRepository;
    private final EmployeeRepository employeeRepository;

    // --- ITEM MANAGEMENT (Admin/HR) ---

    public List<InventoryItem> getAllItems() {
        return inventoryItemRepository.findAll();
    }

    public InventoryItem getItemById(Long id) {
        return inventoryItemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item not found"));
    }

    @Transactional
    public InventoryItem createItem(InventoryItem item) {
        return inventoryItemRepository.save(item);
    }

    @Transactional
    public InventoryItem updateItem(Long id, InventoryItem updatedItem) {
        InventoryItem item = getItemById(id);
        item.setName(updatedItem.getName());
        item.setCategory(updatedItem.getCategory());
        item.setDescription(updatedItem.getDescription());
        item.setStockQuantity(updatedItem.getStockQuantity());
        item.setThreshold(updatedItem.getThreshold());
        item.setStatus(updatedItem.getStatus());
        return inventoryItemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        inventoryItemRepository.deleteById(id);
    }

    // --- REQUEST MANAGEMENT ---

    public List<InventoryRequest> getAllRequests() {
        return inventoryRequestRepository.findAll();
    }

    public List<InventoryRequest> getMyRequests(String username) {
        Employee employee = employeeRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return inventoryRequestRepository.findByEmployeeIdOrderByRequestDateDesc(employee.getId());
    }

    @Transactional
    public InventoryRequest createRequest(String username, Long itemId, Integer quantity, String reason) {
        Employee employee = employeeRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        InventoryItem item = getItemById(itemId);

        // Optional: Check if stock is available right now, or let HR handle it upon
        // approval.
        // Even if stock < quantity, user can request and HR can procure it. Let's allow
        // request anytime.

        InventoryRequest request = new InventoryRequest();
        request.setEmployee(employee);
        request.setItem(item);
        request.setQuantity(quantity);
        request.setReason(reason);
        request.setStatus(InventoryRequest.RequestStatus.PENDING);
        request.setRequestDate(LocalDateTime.now());

        return inventoryRequestRepository.save(request);
    }

    @Transactional
    public InventoryRequest processRequest(Long requestId, String username, InventoryRequest.RequestStatus action,
            String comments) {
        Employee processor = employeeRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        InventoryRequest request = inventoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (action == InventoryRequest.RequestStatus.APPROVED || action == InventoryRequest.RequestStatus.FULFILLED) {
            InventoryItem item = request.getItem();
            if (item.getStockQuantity() < request.getQuantity()) {
                throw new RuntimeException("Insufficient stock to fulfill request.");
            }
            // Deduct stock
            item.setStockQuantity(item.getStockQuantity() - request.getQuantity());
            if (item.getStockQuantity() == 0) {
                item.setStatus(InventoryItem.ItemStatus.OUT_OF_STOCK);
            }
            inventoryItemRepository.save(item);
        } else if (action == InventoryRequest.RequestStatus.RETURNED
                && request.getStatus() == InventoryRequest.RequestStatus.FULFILLED) {
            // Re-add stock
            InventoryItem item = request.getItem();
            item.setStockQuantity(item.getStockQuantity() + request.getQuantity());
            if (item.getStockQuantity() > 0) {
                item.setStatus(InventoryItem.ItemStatus.AVAILABLE);
            }
            inventoryItemRepository.save(item);
        }

        request.setStatus(action);
        request.setComments(comments);
        request.setProcessedBy(processor);
        request.setProcessedAt(LocalDateTime.now());

        return inventoryRequestRepository.save(request);
    }
}
