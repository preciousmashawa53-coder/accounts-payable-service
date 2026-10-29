package zw.co.preciousmashawa.ap.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Role-based access control:
 *   CLERK    captures suppliers and invoices
 *   APPROVER approves or rejects invoices
 *   TREASURY records payments
 * Every endpoint needs authentication; method-level rules sit on the controllers.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                // Stateless API authenticated on every request, so there is no session cookie to forge
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService users(PasswordEncoder encoder,
                             @Value("${app.users.clerk-password}") String clerk,
                             @Value("${app.users.approver-password}") String approver,
                             @Value("${app.users.treasury-password}") String treasury,
                             @Value("${app.users.supervisor-password}") String supervisor) {
        return new InMemoryUserDetailsManager(
                User.withUsername("clerk").password(encoder.encode(clerk)).roles("CLERK").build(),
                User.withUsername("approver").password(encoder.encode(approver)).roles("APPROVER").build(),
                User.withUsername("treasury").password(encoder.encode(treasury)).roles("TREASURY").build(),
                // A supervisor can both capture and approve, which is why the "no self-approval" rule matters
                User.withUsername("supervisor").password(encoder.encode(supervisor)).roles("CLERK", "APPROVER").build());
    }
}
