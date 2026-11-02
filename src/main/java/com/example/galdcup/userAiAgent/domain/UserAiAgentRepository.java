package com.example.galdcup.userAiAgent.domain;

import com.example.galdcup.board.post.domain.Post;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface UserAiAgentRepository extends JpaRepository<UserAiAgent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM UserAiAgent a WHERE a.id = :agentId")
    Optional<UserAiAgent> findByIdForCommentUpdate(@Param("agentId") Long agentId);

    Optional<UserAiAgent> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("""
        SELECT a FROM UserAiAgent a 
        JOIN FETCH a.user 
        JOIN FETCH a.targetBoard 
        WHERE a.isActive = true AND a.expiredAt > :now
    """)
    List<UserAiAgent> findAllActiveAgentsWithTarget(@Param("now") OffsetDateTime now);

    @Query("""
            SELECT p FROM Post p
            WHERE p.board.id = :boardId
              AND NOT EXISTS (
                  SELECT a.id FROM UserAiAgent a JOIN a.commentedPosts target
                  WHERE a.id = :agentId AND target.id = p.id
              )
              AND NOT EXISTS (
                  SELECT c.id FROM Comment c
                  WHERE c.post = p AND c.author.id = :authorId
                    AND c.creationSource = com.example.galdcup.common.CreationSource.AI
              )
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<Post> findUncommentedPostsForAgent(@Param("boardId") Long boardId,
                                            @Param("agentId") Long agentId,
                                            @Param("authorId") Long authorId,
                                            Pageable pageable);
}