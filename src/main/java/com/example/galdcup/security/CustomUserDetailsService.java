package com.example.galdcup.security;

import com.example.galdcup.entity.User;
import com.example.galdcup.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String oauthId) throws UsernameNotFoundException {
        User user = userRepository.findByOauthId(oauthId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        // OAuth 기반 로그인 (password를 사용하지 않음)
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getOauthId())
                .password("{noop}")
                .roles(user.getRole().name())
                .build();
    }
}