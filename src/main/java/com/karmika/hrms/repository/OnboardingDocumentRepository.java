package com.karmika.hrms.repository;

import com.karmika.hrms.entity.OnboardingDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OnboardingDocumentRepository extends JpaRepository<OnboardingDocument, Long> {

    List<OnboardingDocument> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<OnboardingDocument> findByEmployeeIdAndSourceOrderByCreatedAtDesc(
            Long employeeId, OnboardingDocument.DocumentSource source);
}
