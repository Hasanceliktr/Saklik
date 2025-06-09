package org.hasanceliktr.saklik.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor // Parametresiz constructor (Lombok)
@AllArgsConstructor // Tüm alanları içeren constructor (Lombok)
public class MessageResponseDto {
    private String message; // Sadece bir mesaj alanı içeriyor
}