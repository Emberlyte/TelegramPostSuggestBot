package org.example.predlozka_bot2.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.predlozka_bot2.Constants.TelegramBotConstats;
import org.example.predlozka_bot2.Enums.MediaType;
import org.example.predlozka_bot2.Model.Post;
import org.example.predlozka_bot2.Enums.PostStatus;
import org.example.predlozka_bot2.Repository.PostRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional
    public Post savePostSuggestion(
            String content,
            Long senderId,
            String senderName,
            String fileID,
            String filePath,
            String mimeType,
            Long fileSize,
            String caption,
            Integer duration,
            Integer width,
            Integer height,
            String fileUrl,
            MediaType mediaType
    ) {
        Post post = new Post();
        post.setContent(content);
        post.setSenderId(senderId);
        post.setSenderName(senderName);
        post.setCreatedAt(LocalDateTime.now());
        post.setStatus(PostStatus.PENDING);
        post.setFileID(fileID);
        post.setFilePath(filePath);
        post.setMimeType(mimeType);
        post.setFileSize(fileSize);
        post.setCaption(caption);
        post.setDuration(duration);
        post.setWidth(width);
        post.setHeight(height);
        post.setFileUrl(fileUrl);
        post.setMediaType(mediaType);

        Post savedPost = postRepository.save(post);
        log.info("Создан новый пост ID: {} от пользователя: {}", savedPost.getId(), senderId);
        return savedPost;
    }

    @Transactional
    public Optional<Post> updateAdminMessageId(Long postId, Integer adminMessageId) {
        return postRepository.findById(postId).map(post -> {
            post.setAdminMessageId(adminMessageId);
            return postRepository.save(post);
        });
    }

    @Transactional
    public void markPostAsApproved(Long postId) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setStatus(PostStatus.APPROVED);
            post.setApproved(true);
            postRepository.save(post);
            log.info("Пост одобрен. ID поста: {}", postId);
        });
    }

    @Transactional
    public void markPostAsRejected(Long postId) {
        postRepository.findById(postId).ifPresent(post -> {
            post.setStatus(PostStatus.REJECTED);
            post.setApproved(false);
            postRepository.save(post);
            log.info("Пост отклонен. ID поста: {}", postId);
        });
    }

    public Optional<Post> getPostById(Long postId) {
        return postRepository.findById(postId);
    }

    public Optional<Post> getPostByAdminMessageId(Integer adminMessageId) {
        return postRepository.findByAdminMessageId(adminMessageId);
    }

    public List<Post> getPendingPosts() {
        return postRepository.findByStatusOrderByCreatedAtDesc(PostStatus.PENDING);
    }

}
