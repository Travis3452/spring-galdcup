package com.example.galdcup.comment;

import com.example.galdcup.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {
    List<Reply> findByParentCommentId(Long commentId);

    @Modifying
    @Query("update Reply r set r.author = :deletedUser where r.author.id = :userId")
    void updateAuthorToDeletedUser(@Param("userId") Long userId, @Param("deletedUser") User deletedUser);
}