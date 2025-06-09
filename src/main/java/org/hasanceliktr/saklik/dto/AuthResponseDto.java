package org.hasanceliktr.saklik.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
// import java.util.List; // İleride roller için

@Data
@NoArgsConstructor // Lombok parametresiz constructor oluşturur
public class AuthResponseDto {

    private String token;
    private String type = "Bearer"; // Token tipini belirtmek iyi bir pratiktir
    private Long id;
    private String username;
    private String email;
    // private List<String> roles; // İleride roller eklendiğinde

    // AuthService'teki kullanıma uygun constructor:
    public AuthResponseDto(String accessToken, Long id, String username, String email /*, List<String> roles */) {
        this.token = accessToken;
        this.id = id; // BURASI EKSİKTi, ŞİMDİ DOĞRU
        this.username = username;
        this.email = email;
        // this.roles = roles; // İleride
    }

    // Eğer sadece token, username, email ile bir constructor istiyorsan (ve id null kalacaksa),
    // o zaman ayrı bir constructor daha ekleyebilirsin veya bu constructor'ı kullanıp id'yi null geçebilirsin.
    // Ama AuthService'teki kullanım Long id bekliyor.
}