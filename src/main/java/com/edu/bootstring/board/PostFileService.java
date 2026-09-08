package com.edu.bootstring.board;

import com.edu.bootstring.global.error.ErrorCode;
import com.edu.bootstring.global.error.exception.BusinessException;
import com.edu.bootstring.global.error.exception.NotFoundException;
import com.edu.bootstring.global.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 게시글 첨부파일 저장·조회.
 *
 * <p>파일은 {@code app.upload.dir} 아래 {@code yyyy/MM/dd} 로 나눠 담는다.
 * 한 폴더에 수만 개가 쌓이면 파일시스템이 느려진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostFileService {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;   // 10MB

    private final PostFileRepository fileRepository;
    private final BoardRepository boardRepository;
    private final PostService postService;
    private final BoardAccessPolicy accessPolicy;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Transactional
    public List<PostFile> upload(Long postId, List<MultipartFile> files, MemberPrincipal principal) {
        Post post = postService.getPost(postId);
        Board board = boardRepository.findById(post.getBoardId())
                .orElseThrow(() -> new NotFoundException("게시판을 찾을 수 없습니다."));

        accessPolicy.checkFileUpload(board);
        if (!accessPolicy.canModifyPost(post, principal)) {
            throw new BusinessException("이 글에 파일을 올릴 권한이 없습니다.", ErrorCode.ACCESS_DENIED);
        }

        return files.stream()
                .filter(f -> !f.isEmpty())
                .map(f -> store(postId, f))
                .toList();
    }

    private PostFile store(Long postId, MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(
                    "파일 크기는 10MB 를 넘을 수 없습니다. (%s)".formatted(file.getOriginalFilename()),
                    ErrorCode.INVALID_INPUT_VALUE);
        }

        String origName = sanitize(file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString().replace("-", "") + extensionOf(origName);
        String relativeDir = LocalDate.now().format(DATE_PATH);

        try {
            Path dir = Paths.get(uploadDir, relativeDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            Path target = dir.resolve(storedName);
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return fileRepository.save(PostFile.builder()
                    .postId(postId)
                    .origName(origName)
                    .storedName(storedName)
                    .filePath(relativeDir + "/" + storedName)
                    .contentType(file.getContentType())
                    .mediaType(PostFile.resolveMediaType(file.getContentType()))
                    .fileSize(file.getSize())
                    .build());

        } catch (IOException e) {
            log.error("파일 저장 실패 name={} : {}", origName, e.getMessage());
            throw new BusinessException("파일 저장에 실패했습니다: " + origName,
                    ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public PostFile loadForDownload(Long fileId, MemberPrincipal principal) {
        PostFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("파일을 찾을 수 없습니다. id=" + fileId));

        Post post = postService.getPost(file.getPostId());
        Board board = boardRepository.findById(post.getBoardId()).orElseThrow();
        accessPolicy.checkRead(board, principal);

        file.increaseDownloadCount();
        return file;
    }

    public Resource resolveResource(PostFile file) {
        try {
            Path path = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(file.getFilePath());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("파일이 저장소에 없습니다: " + file.getOrigName());
            }
            return resource;
        } catch (IOException e) {
            throw new BusinessException("파일을 읽을 수 없습니다: " + file.getOrigName(),
                    ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    public void delete(Long fileId, MemberPrincipal principal) {
        PostFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new NotFoundException("파일을 찾을 수 없습니다."));
        Post post = postService.getPost(file.getPostId());

        if (!accessPolicy.canModifyPost(post, principal)) {
            throw new BusinessException("이 파일을 삭제할 권한이 없습니다.", ErrorCode.ACCESS_DENIED);
        }
        fileRepository.delete(file);
        // 디스크 파일은 남긴다. 지우다 실패하면 트랜잭션과 어긋나므로 정리는 배치의 몫이다.
    }

    /**
     * 업로드 파일 이름에서 경로 요소를 걷어낸다.
     * {@code ../../etc/passwd} 같은 이름이 그대로 들어오면 저장 경로를 벗어날 수 있다.
     */
    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed";
        }
        String cleaned = Paths.get(name).getFileName().toString();
        return cleaned.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot > -1 ? name.substring(dot) : "";
    }
}
