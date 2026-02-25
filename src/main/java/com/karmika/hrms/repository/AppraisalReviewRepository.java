package com.karmika.hrms.repository;

import com.karmika.hrms.entity.Appraisal;
import com.karmika.hrms.entity.AppraisalReview;
import com.karmika.hrms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppraisalReviewRepository extends JpaRepository<AppraisalReview, Long> {

        List<AppraisalReview> findByAppraisal(Appraisal appraisal);

        List<AppraisalReview> findByReviewer(Employee reviewer);

        List<AppraisalReview> findByAppraisalAndReviewerType(
                        Appraisal appraisal, AppraisalReview.ReviewerType reviewerType);

        Optional<AppraisalReview> findByAppraisalAndReviewerAndReviewerType(
                        Appraisal appraisal, Employee reviewer, AppraisalReview.ReviewerType reviewerType);

        List<AppraisalReview> findByReviewerAndStatus(
                        Employee reviewer, AppraisalReview.ReviewStatus status);

        List<AppraisalReview> findByReviewerAndStatusIn(
                        Employee reviewer, List<AppraisalReview.ReviewStatus> statuses);
}
