package com.sparta.hh99_actualproject.repository;

import com.sparta.hh99_actualproject.model.Follow;
import com.sparta.hh99_actualproject.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow,Long> {
    void deleteByMemberAndFollowMemberId(Member member, String followMemberId);
    Optional<Follow> findByMemberAndFollowMemberId(Member member, String followMemberId);
    List<Follow> findAllByFollowId(String followMemberId);
}
