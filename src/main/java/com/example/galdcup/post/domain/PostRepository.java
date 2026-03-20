package com.example.galdcup.post.domain;

import com.example.galdcup.postCategory.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT p FROM Post p WHERE p.board.id = :boardId " +
            "AND (:categoryId IS NULL OR p.postCategory.id = :categoryId) " +
            "AND (:threshold IS NULL OR p.likeCount >= :threshold)")
    Page<Post> findPostsFiltered(@Param("boardId") Long boardId,
                                 @Param("categoryId") Long categoryId,
                                 @Param("threshold") Long threshold,
                                 Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.board.id = :boardId " +
            "AND (:categoryId IS NULL OR p.postCategory.id = :categoryId) " +
            "AND (:threshold IS NULL OR p.likeCount >= :threshold) " +
            "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> searchByTitleAndContent(@Param("boardId") Long boardId,
                                       @Param("categoryId") Long categoryId,
                                       @Param("threshold") Long threshold,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    @Query("SELECT p FROM Post p WHERE p.board.id = :boardId " +
            "AND (:categoryId IS NULL OR p.postCategory.id = :categoryId) " +
            "AND (:threshold IS NULL OR p.likeCount >= :threshold) " +
            "AND p.author.nickname LIKE %:keyword%")
    Page<Post> searchByAuthorNickname(@Param("boardId") Long boardId,
                                      @Param("categoryId") Long categoryId,
                                      @Param("threshold") Long threshold,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    Page<Post> findByAuthorNickname(String nickname, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.postCategory = :destination WHERE p.postCategory = :target")
    void updateCategoryBulk(@Param("target") PostCategory target,
                            @Param("destination") PostCategory destination);

    @Modifying
    @Query("UPDATE Post post SET post.viewCount = post.viewCount + :views WHERE post.id = :id")
    void incrementViewCount(@Param("id") Long id, @Param("views") long views);
}