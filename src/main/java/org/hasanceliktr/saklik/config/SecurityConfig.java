package org.hasanceliktr.saklik.config;

import org.hasanceliktr.saklik.security.CustomUserDetailsService; // Bizim UserDetailsService'imiz
import org.hasanceliktr.saklik.security.jwt.JwtAuthenticationFilter; // Bizim JWT Filter'ımız
// import org.hasanceliktr.saklik.security.jwt.AuthEntryPointJwt; // Eğer özel bir entry point kullanacaksak (şimdilik varsayılan yeterli olabilir)
import org.springframework.beans.factory.annotation.Autowired; // Eğer field injection veya setter injection tercih ediyorsak
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // CSRF disable için yeni yöntem
import org.springframework.security.config.http.SessionCreationPolicy; // STATELESS session için
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
// import org.springframework.security.web.util.matcher.AntPathRequestMatcher; // Eğer AntPathRequestMatcher kullanacaksak

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // Metod seviyesi güvenlik için (örn: @PreAuthorize)
public class SecurityConfig {

    // CustomUserDetailsService'i doğrudan SecurityFilterChain içinde .userDetailsService() ile set edeceğiz.
    // Bu yüzden burada @Autowired ile inject etmeye gerek yok, ama istersen edebilirsin.
    // private final CustomUserDetailsService customUserDetailsService;

    // JwtAuthenticationFilter'ı @Component yaptığımız için Spring onu bulacak ve bean olarak tanımlayacağız.
    // Ya da burada @Autowired ile inject edip filterChain'e verebiliriz.
    // En temizi, filter'ı bean olarak tanımlayıp filterChain'e vermek.
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Eğer özel bir AuthEntryPoint kullanmayacaksak, buna da gerek yok.
    // Spring Security varsayılan olarak 401 veya 403 döndürür.
    // private final AuthEntryPointJwt unauthorizedHandler;

    // Constructor Injection (Lombok @RequiredArgsConstructor da kullanılabilirdi sınıfta)
    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        // this.customUserDetailsService = customUserDetailsService; // Eğer inject edilecekse
        // this.unauthorizedHandler = unauthorizedHandler; // Eğer inject edilecekse
    }

    // Swagger endpoint'leri için whitelist (WhisperBackend'deki gibi)
    public static final String[] SWAGGER_WHITELIST = {
            "/v3/api-docs/**",      // OpenAPI 3 spec JSON/YAML (genellikle bu kullanılır)
            "/swagger-ui/**",       // Swagger UI HTML, CSS, JS dosyaları
            "/swagger-ui.html"      // Swagger UI ana sayfası
            // "/api-docs/**"       // Eski Swagger 2 için (nadiren gerekir)
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // YENİ: CORS yapılandırmasını buradan al
                .csrf(AbstractHttpConfigurer::disable)
                // ... (sessionManagement, authorizeHttpRequests, addFilterBefore) ...
                .authorizeHttpRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // TÜM OPTIONS İSTEKLERİNE İZİN VER (Preflight için önemli)
                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/hello").authenticated() // /api/hello'yu korumalı yapalım test için
                                .requestMatchers(SWAGGER_WHITELIST).permitAll()
                                .requestMatchers(HttpMethod.POST, "/api/files/upload").authenticated()
                                .requestMatchers(HttpMethod.GET, "/api/files/download/**").authenticated()
                                .requestMatchers(HttpMethod.DELETE, "/api/files/**").authenticated()
                                .anyRequest().authenticated()
                );
        // ...
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); // Frontend origin'i
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")); // İzin verilen tüm metodlar
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type", "X-Requested-With")); // İzin verilen header'lar
        configuration.setAllowCredentials(true); // Cookie/Authorization header'ları için
        configuration.setMaxAge(3600L); // Preflight response'unun ne kadar süreyle cache'leneceği (saniye)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration); // Sadece /api/** altındaki path'ler için bu konfigürasyonu uygula
        // source.registerCorsConfiguration("/**", configuration); // Veya tüm path'ler için
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Şifreleme için BCrypt kullanıyoruz.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        // Spring Boot'un standart AuthenticationManager'ını alıyoruz.
        // Bu, CustomUserDetailsService'imizi ve PasswordEncoder'ımızı otomatik olarak kullanacaktır.
        return authenticationConfiguration.getAuthenticationManager();
    }
}