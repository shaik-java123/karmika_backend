package com.karmika.hrms.controller;

import com.karmika.hrms.entity.Holiday;
import com.karmika.hrms.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayRepository holidayRepository;

    @GetMapping
    public ResponseEntity<List<Holiday>> getAllHolidays() {
        return ResponseEntity.ok(holidayRepository.findAllByOrderByDateAsc());
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Holiday>> getUpcomingHolidays() {
        LocalDate today = LocalDate.now();
        LocalDate endOfYear = today.withDayOfYear(today.lengthOfYear());
        return ResponseEntity.ok(holidayRepository.findByDateBetweenOrderByDateAsc(today, endOfYear));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<?> createHoliday(@RequestBody Holiday holiday) {
        return ResponseEntity.ok(holidayRepository.save(holiday));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<?> updateHoliday(@PathVariable Long id, @RequestBody Holiday holidayDetails) {
        return holidayRepository.findById(id)
                .map(holiday -> {
                    holiday.setName(holidayDetails.getName());
                    holiday.setDate(holidayDetails.getDate());
                    holiday.setType(holidayDetails.getType());
                    holiday.setDescription(holidayDetails.getDescription());
                    return ResponseEntity.ok(holidayRepository.save(holiday));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<?> deleteHoliday(@PathVariable Long id) {
        return holidayRepository.findById(id)
                .map(holiday -> {
                    holidayRepository.delete(holiday);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Holiday deleted successfully"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
