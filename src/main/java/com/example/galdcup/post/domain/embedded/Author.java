package com.example.galdcup.post.domain.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Author {
    @Column(name = "author_id")
    private Long id;

    @Column(name = "author_nickname", nullable = false, length = 50)
    private String nickname;
}


