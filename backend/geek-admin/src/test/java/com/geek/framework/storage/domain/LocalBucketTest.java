package com.geek.framework.storage.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.geek.common.exception.ServiceException;

class LocalBucketTest {

    @TempDir
    Path tempDir;

    @Test
    void storesAndReadsFilesInsideConfiguredRoot() throws Exception {
        LocalBucket bucket = bucket();
        byte[] content = "qr-content".getBytes();

        bucket.put("upload/qr.png", new MockMultipartFile("file", "qr.png", "image/png", content));

        try (InputStream input = bucket.get("upload/qr.png").getInputStream()) {
            assertArrayEquals(content, input.readAllBytes());
        }
    }

    @Test
    void rejectsPathsThatEscapeConfiguredRoot() {
        LocalBucket bucket = bucket();
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        assertThrows(ServiceException.class, () -> bucket.put("../../outside.txt", file));
        assertThrows(ServiceException.class, () -> bucket.get("../../outside.txt"));
        assertThrows(ServiceException.class, () -> bucket.remove("../../outside.txt"));
    }

    @Test
    void rejectsAbsolutePaths() {
        LocalBucket bucket = bucket();
        String absolutePath = tempDir.resolve("outside.txt").toAbsolutePath().toString();

        assertThrows(ServiceException.class, () -> bucket.get(absolutePath));
    }

    private LocalBucket bucket() {
        return LocalBucket.builder()
                .bucketName("test")
                .basePath(tempDir.resolve("storage").toString())
                .permission("private")
                .api("/files/test")
                .build();
    }
}
