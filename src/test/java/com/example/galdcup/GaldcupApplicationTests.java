package com.example.galdcup;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("application-context")
@EnabledIfEnvironmentVariable(named = "GALDCUP_RUN_CONTEXT_TESTS", matches = "true")
@SpringBootTest
class GaldcupApplicationTests {

	@Test
	void contextLoads() {
	}

}
