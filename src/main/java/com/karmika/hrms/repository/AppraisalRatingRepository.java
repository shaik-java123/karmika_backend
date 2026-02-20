package com.karmika.hrms.repository;

import com.karmika.hrms.entity.AppraisalRating;
import com.karmika.hrms.entity.AppraisalReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppraisalRatingRepository extends JpaRepository<AppraisalRating, Long> {

    List<AppraisalRating> findByReview(AppraisalReview review);

    void deleteByReview(AppraisalReview review);
}
