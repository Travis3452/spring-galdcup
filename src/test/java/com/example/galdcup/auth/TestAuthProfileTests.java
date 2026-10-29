package com.example.galdcup.auth;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TestAuthProfileTests {

    @Test
    void defaultProfileDoesNotRegisterBypassLogin() {
        assertRegistration(false);
    }

    @Test
    void explicitDevProfileRegistersBypassLogin() {
        assertRegistration(true, "dev");
    }

    @Test
    void prodProfileDoesNotRegisterBypassLogin() {
        assertRegistration(false, "prod");
    }

    @Test
    void prodProfileOverridesDevForBypassLogin() {
        assertRegistration(false, "dev", "prod");
    }

    @Test
    void otherNonProdProfileDoesNotRegisterBypassLogin() {
        assertRegistration(false, "staging");
    }

    private void assertRegistration(boolean expected, String... profiles) {
        AuthService authService = mock(AuthService.class);
        // 등록 대상 두 빈만 사용하여 DB, Redis, 스케줄러 및 외부 클라이언트를 시작하지 않음.
        try (var context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().setActiveProfiles(profiles);
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test", Map.of(
                    "spring.profiles.active", String.join(",", profiles),
                    "spring.profiles.default", "default",
                    "cookie.secure", "false",
                    "cookie.sameSite", "Lax",
                    "spring.security.oauth2.client.registration.google.redirect-uri",
                    "http://127.0.0.1/auth/callback/google")));
            context.registerBean(AuthService.class, () -> authService);
            context.register(TestAuthController.class);
            context.refresh();

            assertEquals(expected, context.getBeanNamesForType(TestAuthController.class).length == 1);
            verifyNoInteractions(authService);
        }
    }
}
