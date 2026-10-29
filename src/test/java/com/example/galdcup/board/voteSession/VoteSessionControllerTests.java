package com.example.galdcup.board.voteSession;

import com.example.galdcup.board.vote.VoteService;
import com.example.galdcup.board.vote.request.CreateVoteRequest;
import com.example.galdcup.board.vote.response.VoteDto;
import com.example.galdcup.common.security.CustomUserDetails;
import com.example.galdcup.gemini.GeminiService;
import com.example.galdcup.gemini.opinionAnalysis.OpinionAnalysisService;
import com.example.galdcup.user.domain.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VoteSessionControllerTests {

    private final VoteService votes = mock(VoteService.class);
    private final VoteSessionController controller = new VoteSessionController(
            mock(VoteSessionService.class), votes, mock(GeminiService.class), mock(OpinionAnalysisService.class));
    private final CustomUserDetails user = new CustomUserDetails(
            User.builder().id(7L).role(User.Role.USER).build());

    @Test
    void bodyCannotOverrideSessionInRequestPath() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.createVote(10L, 100L, new CreateVoteRequest(200L, 0), user));
        verifyNoInteractions(votes);
    }

    @Test
    void bodyWithoutSessionIdUsesValidatedPath() {
        VoteDto vote = new VoteDto("100:7", 100L, 7L, 0);
        when(votes.createVote(10L, 100L, 7L, 0)).thenReturn(vote);

        var response = controller.createVote(10L, 100L, new CreateVoteRequest(null, 0), user);

        assertSame(vote, response.getBody());
        verify(votes).createVote(10L, 100L, 7L, 0);
    }
}
