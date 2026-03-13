package learntime.backend.global.config.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CustomPasswordEncoder implements PasswordEncoder {

    private final PasswordEncoder delegate =
            PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Value("${pepper}")
    private String pepper;

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword + pepper);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword + pepper, encodedPassword);
    }
}