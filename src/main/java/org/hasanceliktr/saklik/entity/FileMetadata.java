package org.hasanceliktr.saklik.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "file_metadata")
@Data
@NoArgsConstructor
public class FileMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Otomatik artan ID
    private Long id;

    @Column(nullable = false)
    private String fileName; // Kullanıcının yüklediği orijinal dosya adı

    @Column(nullable = false, unique = true) // Sunucudaki dosya adının benzersiz olması iyi bir pratik
    private String storedFileName; // Sunucuda saklanacak olan (muhtemelen UUID veya zaman damgası ile benzersizleştirilmiş) dosya adı

    @Column(nullable = false)
    private String contentType; // Dosyanın MIME tipi (örn: "image/jpeg", "application/pdf")

    @Column(nullable = false)
    private Long size; // Dosya boyutu (byte cinsinden)

    @Column(nullable = false)
    private String filePath; // Dosyanın sunucudaki tam yolu (ana storage dizini + storedFileName)

    @CreationTimestamp // Kayıt oluşturulduğunda otomatik olarak zaman damgası ekler
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    // Dosyanın sahibi olan kullanıcı ile ilişki
    // Bir kullanıcı birden fazla dosya yükleyebilir (One-to-Many: User -> FileMetadata)
    // Bir dosya sadece bir kullanıcıya ait olabilir (Many-to-One: FileMetadata -> User)
    @ManyToOne(fetch = FetchType.LAZY) // LAZY: User bilgisi sadece gerektiğinde yüklensin
    @JoinColumn(name = "user_id", nullable = false) // users tablosundaki id'ye bağlanacak foreign key
    private User owner; // Dosyanın sahibi olan kullanıcı

    // İleride eklenebilecekler:
    // private String description;
    // private Boolean isPublic;
    // private LocalDateTime lastAccessedAt;
    // private String checksum; // MD5, SHA256 vb. dosya bütünlüğü için

    public FileMetadata(String fileName, String storedFileName, String contentType, Long size, String filePath, User owner) {
        this.fileName = fileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.size = size;
        this.filePath = filePath;
        this.owner = owner;
        // uploadedAt @CreationTimestamp ile otomatik set edilecek
    }
}