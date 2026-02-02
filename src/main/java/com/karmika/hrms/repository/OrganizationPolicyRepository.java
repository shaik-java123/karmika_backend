package com.karmika.hrms.repository;

import com.karmika.hrms.entity.OrganizationPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationPolicyRepository extends JpaRepository<OrganizationPolicy, Long> {
}
