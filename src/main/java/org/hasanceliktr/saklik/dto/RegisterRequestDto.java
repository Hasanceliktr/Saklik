package org.hasanceliktr.saklik.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDto {

    @NotBlank(message = "Kullanıcı adı boş olamaz.")
    @Size(min = 3,max = 50,message = "Kullanıcı adı 3 ile 50 karakter arasında olmalıdır.")
    private String username;

    @NotBlank
    @Email(message = "Geçerli bir e-posta adresi giriniz.")
    @Size(max = 100,message = "E-posta en fazla 100 karakter olabilir.")
    private String email;

    @NotBlank(message = "Şifre kısmı boş olamaz.")
    @Size(min = 6,max = 100,message = "Şifre en az 6 karakter olmalıdır.")
    private String password;

}
