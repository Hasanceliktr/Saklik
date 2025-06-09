package org.hasanceliktr.saklik.service;

import lombok.RequiredArgsConstructor;

import org.hasanceliktr.saklik.dto.FileMetadataDto;
import org.hasanceliktr.saklik.entity.FileMetadata;
import org.hasanceliktr.saklik.entity.User;
import org.hasanceliktr.saklik.repository.FileMetadataRepository;
import org.slf4j.Logger; // Logger importu
import org.slf4j.LoggerFactory; // Logger importu
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Transactional importu

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class); // Logger

    private final FileMetadataRepository fileMetadataRepository;
    private final FileStorageService fileStorageService; // FileStorageService'i enjekte et

    @Override
    @Transactional(readOnly = true)
    public List<FileMetadataDto> getFilesByOwner(User owner) {
        // ... (önceki gibi) ...
        List<FileMetadata> files = fileMetadataRepository.findByOwner(owner);
        return files.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // YENİ METOD IMPLEMENTASYONU
    @Override
    @Transactional // Hem disk hem de veritabanı işlemi, transactional olmalı
    public void deleteFileByStoredFileName(String storedFileName, User owner) {
        logger.info("Kullanıcı {} için dosya silme isteği: {}", owner.getUsername(), storedFileName);

        // 1. Dosya metadata'sını bul ve gerçekten bu kullanıcıya mı ait kontrol et.
        FileMetadata metadata = fileMetadataRepository.findByOwnerAndStoredFileName(owner, storedFileName)
                .orElseThrow(() -> {
                    logger.warn("Silinecek dosya bulunamadı veya kullanıcı {} için yetki yok: {}",
                            owner.getUsername(), storedFileName);
                    // Frontend'e daha anlamlı bir mesaj gitmesi için özel bir exception fırlatılabilir.
                    return new RuntimeException("Dosya bulunamadı veya silme yetkiniz yok.");
                });

        // 2. Fiziksel dosyayı diskten sil.
        boolean physicalFileDeleted = fileStorageService.deleteFile(metadata.getStoredFileName(), owner);

        if (physicalFileDeleted) {
            logger.info("Fiziksel dosya başarıyla silindi: {}", metadata.getFilePath());
        } else {
            // Bu durum, dosya diskte yoksa ama metadata varsa olabilir.
            // Ya da silme sırasında bir I/O hatası olduysa.
            // fileStorageService.deleteFile zaten logluyor, burada ek bir aksiyon düşünülebilir.
            // Şimdilik, metadata'yı silmeye devam edelim. Belki de sadece metadata kalmıştır.
            // Veya burada bir hata fırlatıp işlemi durdurabiliriz.
            // Kritik bir durumsa: throw new RuntimeException("Fiziksel dosya silinemedi: " + metadata.getStoredFileName());
            logger.warn("Fiziksel dosya diskten silinemedi veya bulunamadı: {}", metadata.getFilePath());
        }

        // 3. Dosya metadata'sını veritabanından sil.
        fileMetadataRepository.deleteById(metadata.getId());
        logger.info("Dosya metadata'sı başarıyla silindi (ID: {}): {}", metadata.getId(), metadata.getFileName());
    }


    private FileMetadataDto convertToDto(FileMetadata metadata) {
        // ... (önceki gibi) ...
        String formattedDateTime = "";
        if (metadata.getUploadedAt() != null) {
            try {
                formattedDateTime = metadata.getUploadedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception e) {
                logger.error("convertToDto - Tarih formatlama hatası, file ID {}: {}", metadata.getId(), e.getMessage());
            }
        } else {
            logger.warn("convertToDto - uploadedAt is null for file ID {}", metadata.getId());
        }
        return new FileMetadataDto(
                metadata.getId(),
                metadata.getFileName(),
                metadata.getStoredFileName(),
                metadata.getContentType(),
                metadata.getSize(),
                formattedDateTime
        );
    }
}