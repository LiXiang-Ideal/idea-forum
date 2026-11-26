package com.devcommunity.api.service;

import com.devcommunity.api.common.DomainException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AvatarStorageService {
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 12_000_000L;
    private static final int MAX_IMAGE_DIMENSION = 4096;
    private static final String PUBLIC_URL_PREFIX = "/api/v1/uploads/avatars/";

    private final Path storageDirectory;

    public AvatarStorageService(@Value("${app.upload.avatar-directory:uploads/avatars}") String directory) {
        this.storageDirectory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidAvatar();
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "INVALID_AVATAR_SIZE", "头像文件不能超过 5 MB");
        }

        String extension = validateImage(file);
        String fileName = UUID.randomUUID() + extension;
        Path destination = storageDirectory.resolve(fileName);
        try {
            Files.createDirectories(storageDirectory);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, destination);
            }
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(destination);
            } catch (IOException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new DomainException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "AVATAR_STORAGE_FAILED", "头像保存失败");
        }
        return PUBLIC_URL_PREFIX + fileName;
    }

    private String validateImage(MultipartFile file) {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.getInputStream())) {
            if (input == null) {
                throw invalidAvatar();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidAvatar();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String extension;
                if ("png".equals(format)) {
                    extension = ".png";
                } else if ("jpeg".equals(format) || "jpg".equals(format)) {
                    extension = ".jpg";
                } else {
                    throw invalidAvatar();
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0
                        || width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION
                        || (long) width * height > MAX_IMAGE_PIXELS
                        || reader.read(0) == null) {
                    throw invalidAvatar();
                }
                return extension;
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw invalidAvatar();
        }
    }

    private DomainException invalidAvatar() {
        return new DomainException(HttpStatus.BAD_REQUEST, "INVALID_AVATAR", "请选择有效的 JPEG 或 PNG 图片");
    }
}
