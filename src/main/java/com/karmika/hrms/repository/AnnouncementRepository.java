package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByActiveTrueOrderByCreatedAtDesc();

    List<Announcement> findByActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(LocalDateTime now);
}
