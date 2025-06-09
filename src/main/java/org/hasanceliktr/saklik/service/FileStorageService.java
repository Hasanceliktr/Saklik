package org.hasanceliktr.saklik.service;

import org.hasanceliktr.saklik.entity.User; // User entity'sini import et
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils; // StringUtils.cleanPath için
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct; // @PostConstruct için
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID; // Benzersiz dosya adları için
import java.util.stream.Stream;

@Service
public class FileStorageService {

    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${file.upload-dir}")
    private String uploadDirString; // application.properties'den gelen ana yükleme dizini

    private Path rootStorageLocation; // Ana yükleme dizininin Path nesnesi

    @PostConstruct // Bean oluşturulduktan sonra bu metod çalışacak
    public void init() {
        try {
            this.rootStorageLocation = Paths.get(this.uploadDirString).toAbsolutePath().normalize();
            Files.createDirectories(this.rootStorageLocation); // Ana yükleme dizinini oluştur (eğer yoksa)
            logger.info("Dosya depolama alanı başlatıldı: {}", this.rootStorageLocation.toString());
        } catch (IOException e) {
            logger.error("Dosya depolama alanı başlatılamadı veya oluşturulamadı!", e);
            throw new RuntimeException("Dosya depolama alanı başlatılamadı.", e);
        }
    }

    /**
     * Yüklenen dosyayı belirtilen kullanıcıya ait bir alt dizine kaydeder.
     * Dosya adını çakışmaları önlemek için benzersizleştirir.
     *
     * @param file Yüklenecek MultipartFile
     * @param owner Dosyanın sahibi olan User
     * @return Sunucuda saklanan dosyanın benzersiz adı (uzantısıyla birlikte)
     * @throws IOException Dosya kaydetme sırasında bir hata oluşursa
     */
    public String storeFile(MultipartFile file, User owner) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Boş dosya yüklenemez.");
        }

        // Orijinal dosya adını al ve temizle (güvenlik için)
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFileName.contains("..")) {
            // Bu path traversal saldırılarına karşı bir önlem
            throw new IllegalArgumentException("Dosya adı geçersiz karakterler içeriyor: " + originalFileName);
        }

        // Dosya uzantısını al
        String fileExtension = "";
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < originalFileName.length() - 1) {
            fileExtension = originalFileName.substring(lastDot); // .jpg, .png gibi
        }

        // Benzersiz bir dosya adı oluştur (UUID + uzantı)
        String storedFileName = UUID.randomUUID().toString() + fileExtension;

        // Kullanıcıya özel bir alt dizin oluştur (örn: uploads/saklik-files/user_1/)
        Path userSpecificDir = this.rootStorageLocation.resolve("user_" + owner.getId());
        Files.createDirectories(userSpecificDir); // Kullanıcı dizinini oluştur (eğer yoksa)

        // Hedef dosya yolunu oluştur
        Path destinationFilePath = userSpecificDir.resolve(storedFileName).normalize();

        // Dosya zaten varsa (çok düşük ihtimal ama UUID ile yine de olabilir)
        if (Files.exists(destinationFilePath)) {
            // Farklı bir strateji izlenebilir, örn: storedFileName'e ek bir sayaç eklemek
            // Şimdilik hata fırlatalım veya üzerine yazalım (StandardCopyOption.REPLACE_EXISTING ile)
            // Veya yeni bir UUID üretip tekrar deneyebiliriz.
            // En basit haliyle üzerine yazalım veya hata verelim.
            // Hata vermek daha güvenli olabilir, tekrar deneyin mesajı dönebiliriz.
            // Ama UUID ile çakışma olasılığı çok çok düşük.
            logger.warn("Dosya zaten mevcut, üzerine yazılıyor (UUID çakışması?): {}", destinationFilePath);
        }

        logger.info("Dosya kaydediliyor: {} -> {}", originalFileName, destinationFilePath);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFilePath, StandardCopyOption.REPLACE_EXISTING);
        }

        return storedFileName; // Sadece dosya adını (uzantısıyla) döndür, yolu değil.
        // Tam yolu FileMetadata'ya kaydederken oluşturacağız.
    }


    /**
     * Belirtilen dosya adıyla dosyayı yükler (Resource olarak).
     *
     * @param storedFileName Sunucuda saklanan dosyanın adı
     * @param owner Dosyanın sahibi (kullanıcıya özel dizini bulmak için)
     * @return Dosya Resource'u
     * @throws MalformedURLException Geçersiz dosya yolu
     */
    public Resource loadFileAsResource(String storedFileName, User owner) throws MalformedURLException {
        Path userSpecificDir = this.rootStorageLocation.resolve("user_" + owner.getId());
        Path filePath = userSpecificDir.resolve(storedFileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());

        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            logger.error("Dosya bulunamadı veya okunamadı: " + filePath);
            throw new RuntimeException("Dosya bulunamadı: " + storedFileName); // Daha iyi bir exception tipi kullanılabilir
        }
    }

    /**
     * Belirtilen dosyayı siler.
     *
     * @param storedFileName Silinecek dosyanın sunucudaki adı
     * @param owner Dosyanın sahibi
     * @return Silme başarılıysa true, değilse false
     */
    public boolean deleteFile(String storedFileName, User owner) {
        try {
            Path userSpecificDir = this.rootStorageLocation.resolve("user_" + owner.getId());
            Path filePath = userSpecificDir.resolve(storedFileName).normalize();
            boolean result = Files.deleteIfExists(filePath);
            if (result) {
                logger.info("Dosya silindi: {}", filePath);
            } else {
                logger.warn("Silinecek dosya bulunamadı: {}", filePath);
            }
            return result;
        } catch (IOException e) {
            logger.error("Dosya silinirken hata oluştu: " + storedFileName, e);
            return false;
        }
    }

    // Belirli bir kullanıcının tüm dosyalarını listelemek için (ileride kullanılabilir)
    public Stream<Path> loadAll(User owner) throws IOException {
        Path userSpecificDir = this.rootStorageLocation.resolve("user_" + owner.getId());
        if (!Files.exists(userSpecificDir)) {
            return Stream.empty();
        }
        // Sadece dosyaları listele, alt dizinleri değil (maxDepth=1)
        return Files.walk(userSpecificDir, 1)
                .filter(path -> !path.equals(userSpecificDir)) // Ana dizini hariç tut
                .filter(Files::isRegularFile) // Sadece dosyaları al
                .map(userSpecificDir::relativize); // Göreceli path'leri döndür (isteğe bağlı)
    }

    // Ana depolama dizininin altındaki kullanıcıya özel tam yolu döndürür.
    // FileMetadata'ya kaydederken bu kullanılabilir.
    public Path getUserSpecificFilePath(User owner, String storedFileName) {
        Path userSpecificDir = this.rootStorageLocation.resolve("user_" + owner.getId());
        return userSpecificDir.resolve(storedFileName).normalize();
    }
}