package org.example.predlozka_bot2.Repository;

import org.example.predlozka_bot2.Model.Post;
import org.example.predlozka_bot2.Enums.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByStatusOrderByCreatedAtDesc(PostStatus status);

    Optional<Post> findByAdminMessageId(Integer adminMessageId);
}
