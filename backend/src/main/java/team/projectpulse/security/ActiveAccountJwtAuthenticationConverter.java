package team.projectpulse.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import team.projectpulse.user.UserRepository;

/**
 * Turns a verified JWT into an authentication, but only while the account it names is still active
 * (FR-SEC-active-account).
 *
 * <p>The token is self-contained for identity and authorities, which come from the wrapped
 * {@link JwtAuthenticationConverter}. Account status is not: a deactivated or deleted account must lose access on its
 * next request, not when its token expires. So every bearer request reloads the user named by the token's
 * {@code userId} claim, one primary-key read, and throws {@link DisabledException} if that user is gone or disabled.
 * {@code JwtAuthenticationProvider} lets the exception propagate to {@code BearerTokenAuthenticationFilter}, whose
 * entry point answers {@code 401} (KD-self-issued-jwt amendment in the architecture-of-record).
 */
@Component
public class ActiveAccountJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final UserRepository userRepository;


    public ActiveAccountJwtAuthenticationConverter(JwtAuthenticationConverter jwtAuthenticationConverter, UserRepository userRepository) {
        this.jwtAuthenticationConverter = jwtAuthenticationConverter;
        this.userRepository = userRepository;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        AbstractAuthenticationToken authentication = this.jwtAuthenticationConverter.convert(jwt);

        // JwtProvider writes the claim as an Integer; a token without one names no account and is refused like one
        // whose account is gone.
        Object claim = jwt.getClaim("userId");
        if (!(claim instanceof Number userId)) {
            throw new DisabledException("The access token names no user account.");
        }
        boolean active = this.userRepository.findById(userId.intValue())
                .map(user -> user.isEnabled())
                .orElse(false);
        if (!active) {
            throw new DisabledException("This user account has been deactivated.");
        }
        return authentication;
    }

}
