package runrush.be.s3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import runrush.be.common.exception.BusinessException;
import runrush.be.common.exception.ErrorCode;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URL;
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
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "파일 업로드 중 오류가 발생했습니다.");
        } catch (S3Exception e) {
            log.error("S3 업로드 실패", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "S3 업로드에 실패했습니다: " + e.awsErrorDetails().errorMessage());
        }

    }

    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            log.warn("삭제할 이미지 URL이 없습니다.");
            return;
        }

        try {
            String key = extractKeyFromUrl(imageUrl);
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
            log.info("S3 이미지 삭제 성공: {}", imageUrl);

        } catch (BusinessException e) {
            throw e;
        } catch (S3Exception e) {
            log.error("S3 이미지 삭제 실패: {}", imageUrl, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED, "이미지 삭제에 실패했습니다.");
        } catch (Exception e) {
            log.error("예상치 못한 이미지 삭제 오류: {}", imageUrl, e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "이미지 삭제 중 예상치 못한 오류가 발생했습니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "파일이 존재하지 않습니다.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".") || !hasValidExtension(fileName)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT, "지원하지 않는 파일 형식입니다. (jpg, jpeg, png, gif)");
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

    private String extractKeyFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            String path = url.getPath();

            if (path == null || path.isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "URL 경로가 없습니다.");
            }

            return path.startsWith("/") ? path.substring(1) : path;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "잘못된 이미지 URL 형식입니다: " + imageUrl);
        }
    }
}