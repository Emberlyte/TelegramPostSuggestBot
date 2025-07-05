package org.example.predlozka_bot2.Repository;

import org.example.predlozka_bot2.Enums.AppealBanUserStatus;
import org.example.predlozka_bot2.Model.BannedUsers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BannedUsersRepository extends JpaRepository<BannedUsers, Long> {
    Optional<BannedUsers> findBannedUsersBySenderId(Long senderId);

    Optional<BannedUsers> findBannedUsersBySenderIdAndAppealBanUserStatusIsNotNull(Long senderId);

    Long deleteBannedUsersBySenderId(Long senderId);
}
