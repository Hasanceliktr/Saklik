package org.hasanceliktr.saklik.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdDate;
}
