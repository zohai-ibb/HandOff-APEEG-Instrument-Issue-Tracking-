package com.example.APEEG.security;

import com.example.APEEG.model.Person;
import com.example.APEEG.service.PersonService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Filter that intercepts incoming HTTP requests to extract and validate bearer tokens,
 * populating the Spring SecurityContext if a valid token is present.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final PersonService personService;

    // Inject required components via constructor
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, PersonService personService) {
        this.tokenProvider = tokenProvider;
        this.personService = personService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // Extract raw JWT string from HTTP Authorization header
            String jwt = getJwtFromRequest(request);

            // Validate token signature and expiration
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String personId = tokenProvider.getPersonIdFromToken(jwt);

                // Fetch corresponding scientist document from MongoDB
                Optional<Person> personOpt = personService.getPersonById(personId);
                if (personOpt.isPresent()) {
                    Person person = personOpt.get();

                    // Create authentication object storing Person principal
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(person, null, Collections.emptyList());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authenticated context for downstream request processing
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not evaluate user security context", ex);
        }

        // Proceed to next filter in chain
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts token substring from 'Authorization: Bearer <TOKEN>' header.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}