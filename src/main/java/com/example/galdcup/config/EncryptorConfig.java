package com.example.galdcup.config;

import com.example.galdcup.security.AES256Encryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptorConfig {
    @Bean
    public AES256Encryptor aes256Encryptor(@Value("${aes256.key}") String base64Key) {
        return AES256Encryptor.fromBase64Key(base64Key);
    }
}
