package org.example.predlozka_bot2.Model;

import jakarta.persistence.*;
import lombok.Data;
import org.example.predlozka_bot2.Enums.MediaType;
import org.example.predlozka_bot2.Enums.PostStatus;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 4096)
    private String content;

    private Long senderId;
    private String senderName;

    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private PostStatus status = PostStatus.PENDING;

    private Integer adminMessageId;

    @Column(nullable = false)
    private Boolean approved = false;

    @Column(length = 255)
    private String fileID;

    @Column(length = 255)
    private String filePath;

    @Column(length = 100)
    private String mimeType;

    @Column
    private Long fileSize;

    @Column(length = 4096)
    private String caption;

    private Integer duration;
    private Integer width;
    private Integer height;

    @Column(length = 500)
    private String fileUrl;

    private MediaType mediaType;
}
