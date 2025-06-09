package org.hasanceliktr.saklik.controller;

import lombok.RequiredArgsConstructor;
import org.hasanceliktr.saklik.dto.FileMetadataDto;
import org.hasanceliktr.saklik.dto.MessageResponseDto; // Basit mesajlar için
import org.hasanceliktr.saklik.entity.FileMetadata;
import org.hasanceliktr.saklik.entity.User;
import org.hasanceliktr.saklik.repository.FileMetadataRepository;
import org.hasanceliktr.saklik.repository.UserRepository; // User'ı bulmak için (veya SecurityContext'ten alacağız)
import org.hasanceliktr.saklik.service.FileService;
import org.hasanceliktr.saklik.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // Giriş yapmış kullanıcıyı almak için
import org.springframework.security.core.userdetails.UserDetails; // UserDetails'e cast etmek için
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/files") // Dosya ile ilgili tüm endpoint'ler /api/files altında olacak
@RequiredArgsConstructor
// @CrossOrigin(origins = "http://localhost:5173") // Eğer SecurityConfig'de global CORS yoksa veya spesifik ayar gerekiyorsa
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final FileStorageService fileStorageService;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository; // User entity'sini UserDetails'ten almak yerine doğrudan DB'den çekmek için
    private final FileService fileService;


    // --- FileMetadataDto (Henüz Oluşturulmadı) ---
    // Bu DTO'yu org.hasanceliktr.saklik.dto paketine ekleyeceğiz.
    // Şimdilik FileMetadata entity'sini doğrudan döndürebiliriz veya basit bir mesaj.
    // Örnek DTO:
    /*
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadFileResponseDto { // Veya FileMetadataDto
        private Long id;
        private String fileName;
        private String storedFileName;
        private String contentType;
        private Long size;
        private String downloadUrl; // İleride indirme linki
        private LocalDateTime uploadedAt;
    }
    */

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        // @AuthenticationPrincipal UserDetails: Spring Security'nin SecurityContext'inden
        // o anki authenticate olmuş kullanıcının UserDetails nesnesini doğrudan enjekte eder.
        // Bu, CustomUserDetailsService'imizin döndürdüğü nesne olacak.

        if (userDetails == null) {
            // Bu durumun olmaması lazım, çünkü endpoint SecurityConfig'de authenticated() olarak işaretli.
            // Ama bir güvenlik önlemi olarak kontrol edebiliriz.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponseDto("Hata: Bu işlem için giriş yapmalısınız."));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(new MessageResponseDto("Hata: Yüklenecek dosya boş olamaz."));
        }

        try {
            // UserDetails'ten username'i alıp User entity'sini veritabanından çekiyoruz.
            // Neden? Çünkü FileMetadata entity'si User entity'si ile ilişki kuruyor.
            // Alternatif: Eğer kendi UserDetailsImpl'imizi kullanıyor olsaydık ve o User entity'sini
            // veya en azından ID'sini içerseydi, bu sorguya gerek kalmazdı.
            // WhisperBackend'deki CustomUserDetailsService Spring User döndürdüğü için bu sorgu gerekli.
            User owner = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userDetails.getUsername() + ". Sistem hatası."));

            // 1. Dosyayı fiziksel olarak diske kaydet
            String storedFileName = fileStorageService.storeFile(file, owner);

            // 2. Dosyanın sunucudaki tam yolunu al
            Path filePathOnServer = fileStorageService.getUserSpecificFilePath(owner, storedFileName);

            // 3. Dosya metadata'sını oluştur
            FileMetadata metadata = new FileMetadata(
                    StringUtils.cleanPath(file.getOriginalFilename()), // Orijinal adı
                    storedFileName,                                   // Sunucudaki adı
                    file.getContentType(),                            // İçerik tipi
                    file.getSize(),                                   // Boyutu
                    filePathOnServer.toString(),                      // Sunucudaki tam yolu
                    owner                                             // Sahibi
            );

            // 4. Metadata'yı veritabanına kaydet
            FileMetadata savedMetadata = fileMetadataRepository.save(metadata);
            logger.info("Dosya başarıyla yüklendi ve metadata kaydedildi: {} (ID: {}) by User: {}",
                    savedMetadata.getFileName(), savedMetadata.getId(), owner.getUsername());

            // Başarılı response olarak kaydedilen metadata'yı (veya bir DTO'sunu) döndür
            // Şimdilik basit bir mesaj veya metadata'nın kendisi. İleride bir DTO oluşturacağız.
            // Örnek bir DTO dönüşü:
            /*
            UploadFileResponseDto responseDto = new UploadFileResponseDto(
                    savedMetadata.getId(),
                    savedMetadata.getFileName(),
                    savedMetadata.getStoredFileName(),
                    savedMetadata.getContentType(),
                    savedMetadata.getSize(),
                    "/api/files/download/" + savedMetadata.getStoredFileName(), // Örnek indirme URL'i
                    savedMetadata.getUploadedAt()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
            */
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new MessageResponseDto("Dosya başarıyla yüklendi: " + savedMetadata.getFileName()));

        } catch (IllegalArgumentException e) {
            logger.warn("Dosya yükleme hatası (geçersiz argüman): {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponseDto("Hata: " + e.getMessage()));
        } catch (IOException e) {
            logger.error("Dosya yükleme sırasında bir G/Ç hatası oluştu.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponseDto("Hata: Dosya yüklenirken bir sunucu hatası oluştu."));
        } catch (Exception e) {
            logger.error("Dosya yükleme sırasında beklenmedik bir hata oluştu.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponseDto("Hata: Beklenmedik bir sunucu hatası oluştu."));
        }
    }

    @GetMapping
    public ResponseEntity<List<FileMetadataDto>> listFilesForCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // Veya body ile mesaj
        }

        try {
            User owner = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userDetails.getUsername()));

            List<FileMetadataDto> files = fileService.getFilesByOwner(owner);
            logger.info("Kullanıcı {} için {} adet dosya listelendi.", owner.getUsername(), files.size());
            return ResponseEntity.ok(files);
        } catch (RuntimeException e) {
            logger.error("Dosya listeleme sırasında hata oluştu (kullanıcı: {}): {}", userDetails.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Veya body ile mesaj
        }
    }
    @GetMapping("/download/{storedFileName:.+}") // :.+ path variable'ın dosya uzantısını da almasını sağlar
    public ResponseEntity<Resource> downloadFile(@PathVariable String storedFileName,
                                                 @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            // Bu durum SecurityConfig'de .authenticated() ile engellenmeli
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            User owner = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı: " + userDetails.getUsername()));

            // Güvenlik Kontrolü: Dosya gerçekten bu kullanıcıya mı ait?
            // FileMetadata'dan storedFileName ve owner ile kontrol edelim.
            FileMetadata metadata = fileMetadataRepository.findByOwnerAndStoredFileName(owner, storedFileName)
                    .orElseThrow(() -> {
                        logger.warn("Kullanıcı {} için dosya indirme yetkisi yok veya dosya bulunamadı: {}", owner.getUsername(), storedFileName);
                        return new RuntimeException("Dosya bulunamadı veya indirme yetkiniz yok."); // Daha spesifik bir exception
                    });

            Resource resource = fileStorageService.loadFileAsResource(storedFileName, owner);

            // Dosyanın content type'ını belirlemeye çalışalım (metadata'dan)
            String contentType = metadata.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream"; // Bilinmiyorsa varsayılan
            }

            logger.info("Kullanıcı {} dosyasını indiriyor: {} (Orijinal Ad: {})",
                    owner.getUsername(), storedFileName, metadata.getFileName());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    // Content-Disposition header'ı, tarayıcının dosyayı nasıl ele alacağını söyler.
                    // "attachment" dosyayı indirmeye zorlar, "inline" ise tarayıcıda göstermeye çalışır (PDF, resim gibi).
                    // Orijinal dosya adını da buraya eklemek iyi bir kullanıcı deneyimi sunar.
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                    // .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getFileName() + "\"") // Tarayıcıda göstermek için
                    .body(resource);

        } catch (MalformedURLException ex) {
            logger.error("Dosya URL hatası (MalformedURLException) [{}]: {}", storedFileName, ex.getMessage());
            return ResponseEntity.badRequest().build(); // Veya bir mesajla
        } catch (RuntimeException ex) { // Dosya bulunamadı veya yetki yok (yukarıdaki orElseThrow'dan)
            // logger.error("Dosya indirme hatası (RuntimeException) [{}]: {}", storedFileName, ex.getMessage());
            // Zaten loglandı, burada sadece response dönelim
            if (ex.getMessage() != null && ex.getMessage().contains("indirme yetkiniz yok")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // Veya mesajla
            }
            return ResponseEntity.notFound().build(); // Veya mesajla
        } catch (Exception ex) { // Diğer beklenmedik hatalar
            logger.error("Dosya indirme sırasında beklenmedik genel bir hata oluştu [{}]: {}", storedFileName, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    @DeleteMapping("/{storedFileName}")
    public ResponseEntity<MessageResponseDto> deleteFile(@PathVariable String storedFileName,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            // Bu genellikle Spring Security tarafından yakalanır ve 401 döndürülür,
            // ama ek bir kontrol olarak kalabilir.
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponseDto("Yetkisiz işlem: Lütfen giriş yapın."));
        }

        User owner = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> {
                    // Bu durum, authenticate olmuş bir kullanıcının DB'de bulunamaması anlamına gelir,
                    // ki bu ciddi bir tutarsızlık olurdu.
                    logger.error("Kimliği doğrulanmış kullanıcı DB'de bulunamadı: {}", userDetails.getUsername());
                    return new RuntimeException("Kullanıcı sistemde bulunamadı. Lütfen tekrar giriş yapmayı deneyin.");
                });

        // 1. Silinecek dosyanın metadata'sını bul
        FileMetadata fileMetadataToDelete = fileMetadataRepository.findByOwnerAndStoredFileName(owner, storedFileName)
                .orElseThrow(() -> {
                    logger.warn("Kullanıcı {} için silinecek dosya bulunamadı veya yetkisi yok: {}", owner.getUsername(), storedFileName);
                    return new RuntimeException("Dosya bulunamadı veya bu dosyayı silme yetkiniz yok.");
                });

        // 2. Orijinal dosya adını bir değişkene al (response mesajı için)
        String originalFileName = fileMetadataToDelete.getFileName();
        String actualStoredFileName = fileMetadataToDelete.getStoredFileName(); // Diskteki adı da alalım

        try {
            // 3. Dosyayı diskten sil
            boolean deletedFromDisk = fileStorageService.deleteFile(actualStoredFileName, owner);

            if (deletedFromDisk) {
                // 4. Metadata'yı veritabanından sil
                fileMetadataRepository.delete(fileMetadataToDelete); // veya deleteById(fileMetadataToDelete.getId())
                logger.info("Dosya başarıyla silindi (disk ve DB): {}, Orijinal Adı: {}", actualStoredFileName, originalFileName);
                return ResponseEntity.ok(new MessageResponseDto("Dosya '" + originalFileName + "' başarıyla silindi."));
            } else {
                // Dosya diskten silinemedi (belki zaten yoktu veya bir izin sorunu oldu)
                // Bu durumu nasıl ele alacağımıza karar vermeliyiz.
                // Eğer dosya diskte yoksa ama DB'de kaydı varsa, DB kaydını silmek mantıklı olabilir.
                logger.warn("Dosya diskten silinemedi (veya bulunamadı): {}. DB kaydı kontrol ediliyor.", actualStoredFileName);

                // Dosyanın diskte gerçekten olup olmadığını kontrol edelim
                Path filePathOnDisk = fileStorageService.getUserSpecificFilePath(owner, actualStoredFileName);
                if (!Files.exists(filePathOnDisk)) {
                    logger.info("Dosya diskte mevcut değil. Veritabanı kaydı siliniyor: {}", originalFileName);
                    fileMetadataRepository.delete(fileMetadataToDelete);
                    return ResponseEntity.ok(new MessageResponseDto("Dosya diskte bulunamadı ancak veritabanı kaydı başarıyla silindi: '" + originalFileName + "'."));
                } else {
                    // Dosya diskte var ama silinemedi (örn: izin sorunu)
                    logger.error("Dosya diskte mevcut ancak silinemedi: {}", actualStoredFileName);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new MessageResponseDto("Dosya '" + originalFileName + "' diskten silinirken bir hata oluştu."));
                }
            }
        } catch (Exception e) { // fileStorageService.deleteFile veya fileMetadataRepository.delete'den gelebilecek beklenmedik hatalar
            logger.error("Dosya silme işlemi sırasında beklenmedik bir hata oluştu. Dosya: {}, Hata: {}", originalFileName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponseDto("Dosya '" + originalFileName + "' silinirken beklenmedik bir hata oluştu."));
        }
    }

    // İleride eklenecekler:
    // GET /api/files -> Giriş yapmış kullanıcının dosyalarını listele
    // GET /api/files/download/{storedFileName} -> Dosyayı indir
    // DELETE /api/files/{fileIdOrStoredFileName} -> Dosyayı sil
}