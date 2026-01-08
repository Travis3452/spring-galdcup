package com.example.galdcup.vote.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoteOption {

    @Column(nullable = false)
    private String label;

    @Column(nullable = false)
    private String imageUrl;
}