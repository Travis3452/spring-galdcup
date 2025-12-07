package com.example.galdcup.service;

import com.example.galdcup.entity.GaldcupTopic;
import com.example.galdcup.repository.GaldcupTopicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GaldcupTopicService {
    private final GaldcupTopicRepository topicRepository;

    public Optional<GaldcupTopic> findById(Long id) {
        return topicRepository.findById(id);
    }

    public GaldcupTopic save(GaldcupTopic topic) {
        return topicRepository.save(topic);
    }

}
