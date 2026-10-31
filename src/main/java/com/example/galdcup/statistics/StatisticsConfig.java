package com.example.galdcup.statistics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class StatisticsConfig {
    @Bean
    public Clock statisticsClock() { return Clock.systemUTC(); }
}
