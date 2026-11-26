package com.devcommunity.api.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.devcommunity.api.common.DomainException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class AvatarStorageServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void storesDecodablePngWithGeneratedName() throws Exception {
        byte[] imageBytes = pngImage();
        AvatarStorageService service = new AvatarStorageService(tempDirectory.toString());

        String avatarUrl = service.store(new MockMultipartFile(
                "file", "user-supplied-name.png", "image/png", imageBytes));

        String fileName = avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1);
        org.junit.jupiter.api.Assertions.assertTrue(fileName.matches("[0-9a-f-]{36}\\.png"));
        try (Stream<Path> files = Files.list(tempDirectory)) {
            assertArrayEquals(imageBytes, Files.readAllBytes(files.findFirst().orElseThrow()));
        }
    }

    @Test
    void rejectsNonImageContent() {
        AvatarStorageService service = new AvatarStorageService(tempDirectory.toString());
        var file = new MockMultipartFile("file", "image.png", "image/png", new byte[] {1, 2, 3});

        DomainException exception = assertThrows(DomainException.class, () -> service.store(file));

        assertEquals("INVALID_AVATAR", exception.getCode());
    }

    @Test
    void rejectsFilesOverTheConfiguredLimit() {
        AvatarStorageService service = new AvatarStorageService(tempDirectory.toString());
        var file = new MockMultipartFile("file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);

        DomainException exception = assertThrows(DomainException.class, () -> service.store(file));

        assertEquals("INVALID_AVATAR_SIZE", exception.getCode());
    }

    private byte[] pngImage() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
