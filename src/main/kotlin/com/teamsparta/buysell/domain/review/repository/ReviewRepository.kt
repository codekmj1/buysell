package com.teamsparta.buysell.domain.review.repository

import com.teamsparta.buysell.domain.review.model.Review
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface ReviewRepository: JpaRepository<Review, Int> {

    fun findByPostIdAndId(postId: Int, reviewId: Int): Review

    @Query ("SELECT AVG(r.rating) FROM Review r WHERE r.member.id = :memberId")
    fun getAverageRatingByMember(memberId: Int): Double
}