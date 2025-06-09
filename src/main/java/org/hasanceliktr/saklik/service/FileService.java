package org.hasanceliktr.saklik.service;

import org.hasanceliktr.saklik.dto.FileMetadataDto;
import org.hasanceliktr.saklik.entity.User;

import java.util.List;

public interface FileService {
    List<FileMetadataDto> getFilesByOwner(User owner);
    void deleteFileByStoredFileName(String storedFileName, User owner);

}
