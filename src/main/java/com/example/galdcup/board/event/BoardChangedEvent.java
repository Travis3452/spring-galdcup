package com.example.galdcup.board.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BoardChangedEvent {
    private final Long boardId;
}