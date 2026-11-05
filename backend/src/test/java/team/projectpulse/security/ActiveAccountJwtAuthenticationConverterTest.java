package team.projectpulse.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import team.projectpulse.student.Student;
import team.projectpulse.user.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ActiveAccountJwtAuthenticationConverterTest {

    @Mock
    UserRepository userRepository;

    ActiveAccountJwtAuthenticationConverter converter;


    @BeforeEach
    void setUp() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
        delegate.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        this.converter = new ActiveAccountJwtAuthenticationConverter(delegate, this.userRepository);
    }

    private static Jwt jwtFor(Object userId) {
        Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "RS256").subject("j.smith@abc.edu")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                .claim("authorities", "ROLE_student");
        if (userId != null) {
            builder.claim("userId", userId);
        }
        return builder.build();
    }

    private static Student student(boolean enabled) {
        return new Student("j.smith@abc.edu", "John", "Smith", "j.smith@abc.edu", "123456", enabled, "student");
    }

    @Test
    @DisplayName("An active account authenticates with the token's authorities")
    void activeAccountAuthenticates() {
        given(this.userRepository.findById(4)).willReturn(Optional.of(student(true)));

        AbstractAuthenticationToken authentication = this.converter.convert(jwtFor(4));

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting(Object::toString).contains("ROLE_student");
    }

    @Test
    @DisplayName("A deactivated account is refused with DisabledException")
    void deactivatedAccountIsRefused() {
        given(this.userRepository.findById(4)).willReturn(Optional.of(student(false)));

        assertThat(catchThrowable(() -> this.converter.convert(jwtFor(4)))).isInstanceOf(DisabledException.class);
    }

    @Test
    @DisplayName("A deleted account is refused with DisabledException")
    void deletedAccountIsRefused() {
        given(this.userRepository.findById(4)).willReturn(Optional.empty());

        assertThat(catchThrowable(() -> this.converter.convert(jwtFor(4)))).isInstanceOf(DisabledException.class);
    }

    @Test
    @DisplayName("A token without a userId claim is refused without a lookup")
    void tokenWithoutUserIdIsRefused() {
        assertThat(catchThrowable(() -> this.converter.convert(jwtFor(null)))).isInstanceOf(DisabledException.class);
        verifyNoInteractions(this.userRepository);
    }

}
