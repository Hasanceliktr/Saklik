// src/types/fileTypes.ts
export interface FileMetadata { // Backend'deki DTO ile aynı alanlar
    id: number; // Long backend'de, number frontend'de
    fileName: string;
    storedFileName: string;
    contentType: string;
    size: number; // Long backend'de, number frontend'de
    uploadedAt: string; // LocalDateTime backend'de, string olarak alıp Date'e çevirebiliriz
}