package com.example.galdcup.board.board.ban;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.security.access.AccessDeniedException;
import java.time.OffsetDateTime;
@Component @RequiredArgsConstructor
public class BoardWriteGuard {
 private final BoardBanRepository bans;
 public void requireAllowed(Long boardId,Long userId){
  if(bans.countActive(boardId,userId,OffsetDateTime.now())>0) throw new AccessDeniedException("이 게시판에서 글과 댓글 작성이 제한되어 있습니다.");
 }
}
