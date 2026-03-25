package com.example.galdcup.board.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게시판 정보가 변경되었음을 알리는 도메인 이벤트.
 * * @implNote Redis 캐시를 무효화하기 위해 사용.
 */
@Getter
@RequiredArgsConstructor
public class BoardChangedEvent {
    private final Long boardId;
}