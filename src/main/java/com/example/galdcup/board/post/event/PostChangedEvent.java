package com.example.galdcup.board.post.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 게시글 정보 변경 발생 시 후속 처리를 위한 이벤트
 */
@Getter
@RequiredArgsConstructor
public class PostChangedEvent {
    private final Long boardId;
    private final Long postId;
}