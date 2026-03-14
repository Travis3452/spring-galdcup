package com.example.galdcup.post.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PostChangedEvent {
    private final Long boardId;
    private final Long postId;
}