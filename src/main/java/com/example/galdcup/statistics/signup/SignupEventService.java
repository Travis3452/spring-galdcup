package com.example.galdcup.statistics.signup;

import com.example.galdcup.statistics.signup.domain.SignupEvent;
import com.example.galdcup.statistics.signup.domain.SignupEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class SignupEventService {

    private final SignupEventRepository signupEventRepository;

    @Transactional
    public void recordSignup(OffsetDateTime signedUpAt) {
        signupEventRepository.save(new SignupEvent(signedUpAt));
    }
}
