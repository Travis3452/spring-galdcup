package com.example.galdcup.statistics;

import com.example.galdcup.user.UserService;
import com.example.galdcup.user.domain.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SignupRecordingTests {
    @Test
    void onlyNewAccountsCreateSignupEventsWithTheirActualCreationTimestamp() {
        UserRepository users = mock(UserRepository.class);
        SignupEventRepository events = mock(SignupEventRepository.class);
        UserNicknameGenerator names = mock(UserNicknameGenerator.class);
        UserService service = new UserService(users, null, null, null, null, names, events);
        when(users.findByOauthId("new-account")).thenReturn(Optional.empty());
        when(names.generate()).thenReturn("새회원");
        when(users.save(any())).thenAnswer(call -> call.getArgument(0));
        User created = service.getOrCreateUser("new-account", "new@example.test");
        ArgumentCaptor<SignupEvent> event = ArgumentCaptor.forClass(SignupEvent.class);
        verify(events).save(event.capture());
        assertNotNull(created.getCreatedAt());
        assertEquals(created.getCreatedAt(), event.getValue().getCreatedAt());
        assertEquals(User.Role.USER, created.getRole());
        when(users.findByOauthId("new-account")).thenReturn(Optional.of(created));
        assertSame(created, service.getOrCreateUser("new-account", "new@example.test"));
        verify(events, times(1)).save(any());
        verify(users, times(1)).save(any());
    }
}
