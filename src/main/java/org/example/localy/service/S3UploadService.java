package org.example.localy.service;

import lombok.RequiredArgsConstructor;
import org.example.localy.config.AppProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Client s3Client;
    private final AppProperties props;

    public String upload(MultipartFile file) {
        String ext = extractExtension(file.getOriginalFilename());
        String key = props.getS3().getRootPrefix() + "/" + UUID.randomUUID() + "." + ext;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(props.getS3().getBucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }

        String cfDomain = props.getS3().getCloudfrontDomain();
        if (cfDomain != null && !cfDomain.isBlank()) {
            return "https://" + cfDomain + "/" + key;
        }
        return "https://" + props.getS3().getBucket()
                + ".s3." + props.getS3().getRegion()
                + ".amazonaws.com/" + key;
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
