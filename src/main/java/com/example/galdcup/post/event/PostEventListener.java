package com.example.galdcup.post.event;

import com.example.galdcup.post.PostRedisManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
class PostEventListener {

    private final PostRedisManager postRedisManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePostChanged(PostChangedEvent event) {
        postRedisManager.deletePostListCache(event.getBoardId());
    }
}
