package org.hasanceliktr.saklik.security;

import lombok.RequiredArgsConstructor;
import org.hasanceliktr.saklik.entity.User; // Bizim User entity'miz
import org.hasanceliktr.saklik.repository.UserRepository; // Bizim UserRepository'miz
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections; // Collections.singletonList için
import java.util.Set;         // Eğer roller Set ise (ileride)
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // final field'lar için constructor oluşturur (Lombok)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository; // @Autowired yerine final ve @RequiredArgsConstructor

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Kullanıcıyı hem kullanıcı adına hem de e-postasına göre arayabiliriz.
        // WhisperBackend'deki gibi yapalım:
        User user = userRepository.findByUsername(usernameOrEmail)
                .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
                        .orElseThrow(() ->
                                new UsernameNotFoundException("Kullanıcı bulunamadı: " + usernameOrEmail)));

        // Rolleri Oluşturma (Saklık projesinde User entity'sinde henüz roles alanı yok)
        // Şimdilik varsayılan bir rol verelim.
        // İleride User entity'sine Role entity'si ile bir ilişki kurduğumuzda,
        // aşağıdaki gibi dinamik rol ataması yapabiliriz:
        /*
        Set<GrantedAuthority> authorities = user
                .getRoles() // User entity'sinde getRoles() metodu olmalı
                .stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().toString())) // Eğer Role bir enum ise .name(), String ise direkt role.getName()
                .collect(Collectors.toSet());
        */
        // Şimdilik varsayılan "ROLE_USER"
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        // Spring'in kendi UserDetails implementasyonu olan org.springframework.security.core.userdetails.User'ı kullanalım.
        // Bu, UserDetailsImpl sınıfını ayrıca yazma ihtiyacını ortadan kaldırır.
        // Ancak, principal'dan id ve email gibi ek bilgilere doğrudan erişmek istersek kendi UserDetailsImpl'imizi yazmak daha iyi olurdu.
        // WhisperBackend'deki yapı Spring'in User'ını kullanıyor, biz de öyle başlayalım.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), // Önemli: Spring Security genellikle 'username' alanını bekler. Email ile login yapsak bile buraya username geçebiliriz.
                // Veya WhisperBackend'deki gibi user.getEmail() de geçebiliriz, ama o zaman JWT'de ve diğer yerlerde hep email'i username olarak ele almalıyız.
                // Tutarlılık için user.getUsername() kullanalım şimdilik.
                user.getPassword(), // Veritabanından gelen hash'lenmiş şifre
                true, // enabled (User entity'mizde isActive alanı yok, şimdilik true)
                true, // accountNonExpired (Şimdilik true)
                true, // credentialsNonExpired (Şimdilik true)
                true, // accountNonLocked (Şimdilik true)
                authorities);
    }
}