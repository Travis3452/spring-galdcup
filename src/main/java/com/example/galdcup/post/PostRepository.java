package com.example.galdcup.post;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByBoardId(Long boardId, Pageable pageable);
    Page<Post> findByAuthorNickname(String nickname, Pageable pageable);
    Page<Post> findByBoardIdAndLikeCountGreaterThanEqual(Long boardId, long likeThreshold, Pageable sortedPageable);

    @Modifying
    @Query("UPDATE Post post SET post.viewCount = post.viewCount + :views WHERE post.id = :id")
    void incrementViewCount(@Param("id") Long id, @Param("views") long views);

    @Query("SELECT p FROM Post p " +
            "WHERE p.board.id = :boardId " +
            "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> searchByBoardAndTitleOrContent(@Param("boardId") Long boardId,
                                       @Param("keyword") String keyword,
                                       Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "WHERE p.board.id = :boardId " +
            "AND p.author.nickname LIKE %:keyword%")
    Page<Post> searchByBoardAndAuthor(@Param("boardId") Long boardId,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "WHERE p.board.id = :boardId " +
            "AND p.likeCount >= :likeThreshold " +
            "AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword%)")
    Page<Post> searchPopularPostsByBoardAndTitleOrContent(@Param("boardId") Long boardId,
                                                          @Param("likeThreshold") Long likeThreshold,
                                                          @Param("keyword") String keyword,
                                                          Pageable sortedPageable);

    @Query("SELECT p FROM Post p " +
            "WHERE p.board.id = :boardId " +
            "AND p.likeCount >= :likeThreshold " +
            "AND p.author.nickname LIKE %:keyword%")
    Page<Post> searchPopularPostsByBoardAndAuthor(@Param("boardId") Long boardId,
                                                  @Param("likeThreshold") Long likeThreshold,
                                                  @Param("keyword") String keyword,
                                                  Pageable sortedPageable);
}
