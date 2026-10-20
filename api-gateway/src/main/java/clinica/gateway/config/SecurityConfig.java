package clinica.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/** Segurança stateless: login público, /api/v1/** exige Bearer JWT, frontend estático público. */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain security(HttpSecurity http, JsonAuthenticationEntryPoint entryPoint) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                // Fora da API só há o frontend estático (/, /index.html, *.css, *.js, ícones) e /error.
                .anyRequest().permitAll())
            .exceptionHandling(e -> e.authenticationEntryPoint(entryPoint))
            // O filtro do resource server valida assinatura e expiração do JWT antes do controller.
            .oauth2ResourceServer(o -> o.jwt(Customizer.withDefaults()).authenticationEntryPoint(entryPoint));
        return http.build();
    }

    @Bean
    SecretKey jwtSecretKey(AppProperties.Jwt p) {
        return new SecretKeySpec(p.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
