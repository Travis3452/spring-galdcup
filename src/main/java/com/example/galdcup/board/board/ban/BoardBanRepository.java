package com.example.galdcup.board.board.ban;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.*;
import java.time.OffsetDateTime;
public interface BoardBanRepository extends JpaRepository<BoardBan,Long>{
 @Query("SELECT COUNT(b) FROM BoardBan b WHERE b.board.id=:board AND b.userId=:user AND b.revokedAt IS NULL AND (b.expiresAt IS NULL OR b.expiresAt>:now)")
 long countActive(@Param("board")Long board,@Param("user")Long user,@Param("now")OffsetDateTime now);
 Page<BoardBan> findByBoardIdOrderByCreatedAtDescIdDesc(Long boardId,Pageable page);
}
