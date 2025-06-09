package org.hasanceliktr.saklik.repository;

import org.hasanceliktr.saklik.entity.FileMetadata;
import org.hasanceliktr.saklik.entity.User; // User entity'sini import et
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {


    List<FileMetadata> findByOwner(User owner);


    Optional<FileMetadata> findByOwnerAndFileName(User owner, String fileName); // Eğer tekil olması bekleniyorsa

    // List<FileMetadata> findAllByOwnerAndFileName(User owner, String fileName); // Eğer birden fazla olabiliyorsa

    // Sunucudaki benzersiz dosya adına (storedFileName) göre dosyayı bul
    Optional<FileMetadata> findByStoredFileName(String storedFileName);

    // Belirli bir kullanıcıya ait ve belirli bir storedFileName'e sahip dosyayı bul (güvenlik için)
    Optional<FileMetadata> findByOwnerAndStoredFileName(User owner, String storedFileName);

    // İleride eklenebilecekler:
    // List<FileMetadata> findByOwnerAndContentTypeContaining(User owner, String contentType);
    // Page<FileMetadata> findByOwner(User owner, Pageable pageable); // Sayfalama için
}