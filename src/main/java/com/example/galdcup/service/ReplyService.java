package com.example.galdcup.service;

import com.example.galdcup.entity.Reply;
import com.example.galdcup.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReplyService {
    private final ReplyRepository replyRepository;

    public Optional<Reply> findById(Long id) {
        return replyRepository.findById(id);
    }

    public Reply save(Reply reply) {
        return replyRepository.save(reply);
    }
}
