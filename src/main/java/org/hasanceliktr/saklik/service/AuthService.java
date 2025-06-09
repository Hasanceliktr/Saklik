package org.hasanceliktr.saklik.service;

import org.hasanceliktr.saklik.dto.AuthResponseDto;    // Login sonrası döneceğimiz DTO
import org.hasanceliktr.saklik.dto.LoginRequestDto;    // Login için istek DTO'su
import org.hasanceliktr.saklik.dto.RegisterRequestDto; // Kayıt için istek DTO'su
// import org.hasanceliktr.saklik.entity.User; // Eğer User entity'si döndürmek istersek

public interface AuthService {

    /**
     * Yeni bir kullanıcıyı sisteme kaydeder.
     * Kullanıcı adı veya e-posta zaten mevcutsa hata fırlatır.
     * Şifreyi hash'leyerek kaydeder.
     *
     * @param registerRequestDto Kayıt için gerekli kullanıcı bilgilerini içeren DTO.
     * @throws RuntimeException Kullanıcı adı veya e-posta zaten kullanımda ise.
     */
    void registerUser(RegisterRequestDto registerRequestDto); // Başarılı kayıtta bir şey döndürmeyebiliriz, Controller mesaj döner.
    // Veya kaydedilen User entity'sini/DTO'sunu dönebilir. Şimdilik void.

    /**
     * Kullanıcının sisteme giriş yapmasını sağlar.
     * Başarılı girişte JWT ve temel kullanıcı bilgilerini içeren bir DTO döndürür.
     *
     * @param loginRequestDto Giriş için kullanıcı adı ve şifreyi içeren DTO.
     * @return Başarılı girişte AuthResponseDto.
     * @throws org.springframework.security.core.AuthenticationException Kimlik doğrulama başarısız olursa (örn: BadCredentialsException).
     */
    AuthResponseDto loginUser(LoginRequestDto loginRequestDto);
}