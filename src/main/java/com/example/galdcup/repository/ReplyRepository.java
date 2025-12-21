package com.example.galdcup.repository;

import com.example.galdcup.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {
    List<Reply> findByParentCommentId(Long commentId);
}