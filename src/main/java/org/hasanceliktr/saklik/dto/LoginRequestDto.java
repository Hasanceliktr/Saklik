package org.hasanceliktr.saklik.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequestDto {

    @NotBlank(message = "Kullanıcı adı veya e-posta boş olamaz.")
    private String username;

    @NotBlank(message = "Şifre boş olamaz.")
    private String password;
}
