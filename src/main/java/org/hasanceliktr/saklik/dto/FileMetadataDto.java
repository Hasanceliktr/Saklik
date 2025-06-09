package org.hasanceliktr.saklik.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataDto {
    private Long id;
    private String fileName;
    private String storedFileName;
    private String contentType;
    private Long size;
    private String uploadedAt;
}
