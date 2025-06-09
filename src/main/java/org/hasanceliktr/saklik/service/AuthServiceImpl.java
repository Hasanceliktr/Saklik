package org.hasanceliktr.saklik.service;

import lombok.RequiredArgsConstructor; // Lombok ile final alanlar için constructor
import org.hasanceliktr.saklik.dto.AuthResponseDto;
import org.hasanceliktr.saklik.dto.LoginRequestDto;
import org.hasanceliktr.saklik.dto.RegisterRequestDto;
import org.hasanceliktr.saklik.entity.User; // User entity'miz
import org.hasanceliktr.saklik.repository.UserRepository; // UserRepository'miz
import org.hasanceliktr.saklik.security.jwt.JwtTokenProvider; // Yeni JwtTokenProvider'ımız
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails; // Spring'in UserDetails'i
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Kayıt işlemi için

@Service
@RequiredArgsConstructor // Lombok
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional // Veritabanına yazma işlemi olduğu için @Transactional eklemek iyi bir pratiktir.
    public void registerUser(RegisterRequestDto registerRequestDto) {
        logger.info("Yeni kullanıcı kaydı deneniyor: {}", registerRequestDto.getUsername());

        if (userRepository.existsByUsername(registerRequestDto.getUsername())) {
            logger.warn("Kullanıcı adı zaten kullanımda: {}", registerRequestDto.getUsername());
            throw new RuntimeException("Hata: Kullanıcı adı zaten kullanımda!"); // Daha spesifik exception'lar kullanılabilir
        }

        if (userRepository.existsByEmail(registerRequestDto.getEmail())) {
            logger.warn("E-posta adresi zaten kullanımda: {}", registerRequestDto.getEmail());
            throw new RuntimeException("Hata: E-posta adresi zaten kullanımda!");
        }

        User user = new User();
        user.setUsername(registerRequestDto.getUsername());
        user.setEmail(registerRequestDto.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        // createdAt alanı User entity'sindeki @CreationTimestamp ile otomatik dolacak

        userRepository.save(user);
        logger.info("Kullanıcı başarıyla kaydedildi: {} (ID: {})", user.getUsername(), user.getId());
    }

    @Override
    public AuthResponseDto loginUser(LoginRequestDto loginRequestDto) {
        logger.info("Kullanıcı girişi deneniyor: {}", loginRequestDto.getUsername());

        // AuthenticationManager ile kullanıcıyı authenticate etmeye çalış
        // Bu, CustomUserDetailsService'i ve PasswordEncoder'ı kullanacaktır.
        // Başarısız olursa AuthenticationException (örn: BadCredentialsException) fırlatır.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequestDto.getUsername(),
                        loginRequestDto.getPassword()
                )
        );

        // Authentication başarılıysa, SecurityContext'e set et
        SecurityContextHolder.getContext().setAuthentication(authentication);
        logger.info("Kullanıcı başarıyla authenticate edildi: {}", loginRequestDto.getUsername());

        // JWT oluştur
        String jwt = jwtTokenProvider.generateToken(authentication);
        logger.debug("JWT üretildi: {}", jwt.substring(0, Math.min(jwt.length(), 20)) + "..."); // Token'ın sadece başını logla

        // Principal'dan UserDetails'i al (Bu Spring'in User nesnesi olacak)
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // AuthResponseDto için User entity'sinden ek bilgilere (id, email) ihtiyacımız var.
        // Çünkü Spring'in User nesnesi bunları doğrudan içermiyor.
        // Bu yüzden, username (veya email) ile User entity'sini tekrar çekiyoruz.
        // Bu, WhisperBackend'deki CustomUserDetailsService'in User entity'sini değil de
        // Spring User'ını döndürme tercihinin bir sonucu.
        User userEntity = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> {
                    // Bu durumun olmaması lazım, çünkü authentication başarılı oldu.
                    // Belki username yerine email ile aramak gerekebilir, CustomUserDetailsService'deki identifier'a bağlı.
                    // CustomUserDetailsService'de principal olarak user.getUsername() kullandığımız için burada da username ile arıyoruz.
                    logger.error("Kimlik doğrulama başarılı olmasına rağmen User entity bulunamadı: {}", userDetails.getUsername());
                    return new RuntimeException("Kullanıcı detayı alınamadı, sistem hatası.");
                });


        return new AuthResponseDto(
                jwt,
                userEntity.getId(),
                userEntity.getUsername(),
                userEntity.getEmail()
                // userDetails.getAuthorities() ... roller string listesine çevrilip eklenebilir
        );
    }
}