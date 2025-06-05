package runrush.be.s3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ImageUploadService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    private final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "gif");

    public String uploadImage(MultipartFile file, String directory) {
        validateFile(file);

        String fileName = generateFileName(file.getOriginalFilename());
        String key = buildS3Key(directory, fileName);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .acl("public-read")
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String imageUrl = s3Client.utilities().getUrl(GetUrlRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()).toExternalForm();

            log.info("이미지 업로드 성공: {}", imageUrl);
            return imageUrl;

        } catch (IOException e) {
            log.error("파일 읽기 실패", e);
            throw new RuntimeException("파일 업로드 중 오류가 발생했습니다.", e);
        } catch (S3Exception e) {
            log.error("S3 업로드 실패", e);
            throw new RuntimeException("S3 업로드에 실패했습니다: " + e.awsErrorDetails().errorMessage(), e);
        }

    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 존재하지 않습니다.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".") || !hasValidExtension(fileName)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif)");
        }
    }

    private boolean hasValidExtension(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    private String generateFileName(String fileName) {
        String substring = fileName.substring(fileName.lastIndexOf("."));
        return UUID.randomUUID() + "_" + System.currentTimeMillis() + substring;
    }

    private String buildS3Key(String directory, String fileName) {
        String datePath = LocalDate.now().toString();
        return directory + "/" + datePath + "/" + fileName;
    }
}