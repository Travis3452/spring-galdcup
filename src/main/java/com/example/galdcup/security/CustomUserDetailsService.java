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
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        Long id;

        try {
            id = Long.valueOf(userId);
        } catch (NumberFormatException e) {
            throw new UsernameNotFoundException("잘못된 사용자 ID 형식입니다: " + userId);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));

        // ✅ CustomUserDetails 반환
        return new CustomUserDetails(user);
    }
}