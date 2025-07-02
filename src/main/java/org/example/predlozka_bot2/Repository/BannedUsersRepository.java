package org.example.predlozka_bot2.Repository;

import org.example.predlozka_bot2.Model.BannedUsers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BannedUsersRepository extends JpaRepository<BannedUsers, Long> {
    Optional<BannedUsers> findBannedUsersBySenderId(Long senderId);
}
