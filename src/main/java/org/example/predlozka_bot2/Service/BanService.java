package org.example.predlozka_bot2.Service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.predlozka_bot2.Enums.AppealBanUserStatus;
import org.example.predlozka_bot2.Enums.BanStatus;
import org.example.predlozka_bot2.Model.BannedUsers;
import org.example.predlozka_bot2.Repository.BannedUsersRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static org.example.predlozka_bot2.Enums.AppealBanUserStatus.*;

@Service
@Slf4j
public class BanService {

    private final BannedUsersRepository bannedUsersRepository;

    public BanService(BannedUsersRepository bannedUsersRepository) {
        this.bannedUsersRepository = bannedUsersRepository;
    }

    public Optional<BannedUsers> getBannedUsersBySenderId(Long senderId) {
        log.debug("Попытка получить данные о: {}", senderId);
        return bannedUsersRepository.findById(senderId);
    }

    public boolean isUserBanned(Long senderId) {
        Optional<BannedUsers> bannedUsers = getBannedUsersBySenderId(senderId);
        return bannedUsers.isPresent();
    }

    @Transactional
    public BannedUsers banUser(Long senderId) {
        log.info("Начата попытка заблокировать пользователя: {}", senderId);
        Optional<BannedUsers> bannedUsers = getBannedUsersBySenderId(senderId);
        BannedUsers banEntity;

        if (bannedUsers.isPresent()) {
            banEntity = bannedUsers.get();
            log.info("Найден существующий статус блокировки для пользователя: {}", senderId);
        } else {
            banEntity = new BannedUsers();
            banEntity.setSenderId(senderId);
            log.info("Создание новой записи о бане для пользователя: {}", senderId);
        }

        banEntity.setStatus(BanStatus.BANNED);

        BannedUsers bannedUsersEntity = bannedUsersRepository.save(banEntity);
        log.info("Блокировка пользователя: {} произошла успешно!", senderId);
        return bannedUsersEntity;
    }

    @Transactional
    public void unBanUser(Long senderId) {
        try {
            log.info("Попытка разблокировать: {}", senderId);
            getBannedUsersBySenderId(senderId).ifPresent(banEntity -> {
                banEntity.setStatus(BanStatus.UNBANNED);
                bannedUsersRepository.save(banEntity);
                log.info("Разблокировка пользователя: {} , произошла успешна!", senderId);

            });
        } catch (Exception e){
            log.error("Ошибка при разблокировке пользователя {}: {}", senderId, e.getMessage());
            throw e;
        }
    }

    @Transactional
    public void deleteBanUser(Long senderId) {
        try {
            if (senderId == null) {
               log.error("ID не может быть ноль");
               return;
            }

            log.info("Попытка удалить забаненного юзера из базы данных: {}", senderId);
            bannedUsersRepository.deleteBannedUsersBySenderId(senderId);
            log.info("Попытка удалить забаненного юзера из базы данных: {} , произошла успешна!", senderId);
        } catch (Exception e){
            log.error("Ошибка при попытке удалить забаненного юзера из базы данных: {}. Причина:{}", senderId, e.getMessage(), e);
            throw e;
        }
    }

    public BannedUsers isUserHasAppeal(Long senderId) {
        log.info("Проверка существующей апелляции для пользователя: {}", senderId);
        Optional<BannedUsers> appealUser = hasAppeal(senderId);

        if (appealUser.isPresent()) {
            BannedUsers appealEntity = appealUser.get();
            log.info("Найдена существующая апелляция для пользователя {}, статус: {}",
                    senderId, appealEntity.getAppealBanUserStatus());
            return appealEntity;
        }

        log.info("Апелляция для пользователя {} не найдена", senderId);
        return null;
    }

    private Optional<BannedUsers> hasAppeal(Long senderId) {
        return bannedUsersRepository.findBannedUsersBySenderIdAndAppealBanUserStatusIsNotNull(senderId);
    }
}
