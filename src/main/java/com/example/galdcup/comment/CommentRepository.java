package com.example.galdcup.comment;

import com.example.galdcup.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByPostId(Long postId, Pageable pageable);
    Page<Comment> findByAuthorNickname(String nickname, Pageable pageable);

    @Modifying
    @Query("update Comment c set c.author = :deletedUser where c.author.id = :userId")
    void updateAuthorToDeletedUser(@Param("userId") Long userId, @Param("deletedUser") User deletedUser);
}