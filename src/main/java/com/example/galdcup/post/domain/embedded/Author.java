package com.example.galdcup.post.domain.embedded;

import com.example.galdcup.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class Author {
    @Column(name = "author_id")
    private Long id;

    @Column(name = "author_nickname", nullable = false, length = 50)
    private String nickname;

    public static Author from(User user) {
        return new Author(user.getId(), user.getNickname());
    }
}