package com.example.galdcup.comment.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Author {
    @Column(name = "author_id")
    private Long id;

    @Column(name = "author_nickname", nullable = false, length = 50)
    private String nickname;
}

