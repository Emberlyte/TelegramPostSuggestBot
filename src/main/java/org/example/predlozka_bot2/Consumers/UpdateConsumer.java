package org.example.predlozka_bot2.Consumers;

import lombok.extern.slf4j.Slf4j;
import org.example.predlozka_bot2.Constants.TelegramBotConstats;
import org.example.predlozka_bot2.Enums.AppealBanUserStatus;
import org.example.predlozka_bot2.Enums.MediaType;
import org.example.predlozka_bot2.Model.BannedUsers;
import org.example.predlozka_bot2.Model.Post;
import org.example.predlozka_bot2.Enums.PostStatus;
import org.example.predlozka_bot2.Repository.BannedUsersRepository;
import org.example.predlozka_bot2.Service.BanService;
import org.example.predlozka_bot2.Service.PostService;
import org.example.predlozka_bot2.Service.RateLimitService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class UpdateConsumer implements LongPollingSingleThreadUpdateConsumer {

    private final TelegramClient telegramClient;
    private final PostService postService;
    private final BanService banService;
    private final String botToken;

    private final Set<Long> usersWritingAppeal = new HashSet<>();

    private final RateLimitService rateLimitService;
    private final BannedUsersRepository bannedUsersRepository;

    @Value("${telegram.channel-id}")
    private Long channelID;

    @Value("${telegram.bot.users.id}")
    private List<Long> adminIDs;

    public UpdateConsumer(@Value("${telegram.bot.token}") String token, PostService postService, BanService banService, RateLimitService rateLimitService, BannedUsersRepository bannedUsersRepository) {
        this.telegramClient = new OkHttpTelegramClient(token);
        this.postService = postService;
        this.botToken = token;
        this.banService = banService;
        this.rateLimitService = rateLimitService;
        this.bannedUsersRepository = bannedUsersRepository;
    }

    @Override
    public void consume(Update update) {
        try {
            if (update.hasMessage()) {
                handleMessage(update.getMessage());
            } else if (update.hasCallbackQuery()) {
                handleCallBackQuery(update.getCallbackQuery());
            }
        } catch (Exception e) {
            log.error("ошибка при получении апдейта", e);
        }
    }

    private void handleMessage(Message message) {
        if (message == null) {
            log.debug("Пустое сообщение");
            return;
        }

        Long chatId = message.getChatId();
        String text = message.hasText() ? message.getText() : null;
        Long senderId = message.getFrom().getId();
        String senderUsername = message.getFrom().getUserName() != null ? message.getFrom().getUserName() : message.getFrom().getFirstName();


        if (!rateLimitService.isAllowed(chatId, 5, 60)){
            sendMessage(chatId, TelegramBotConstats.MSG_HAS_TO_MANY_REQUEST);
            log.warn("Превышен rate-limit от: {} (chatID:{})", senderUsername, chatId);
            return;
        }

        if (banService.isUserBanned(senderId)){
            sendBanMenuToUser(chatId);
            log.info("Забаненный юзер {} (Id: {}) использовал бота. ChatId: {}", senderUsername, senderId, chatId);

            sendAppealMenu(text, chatId, senderUsername, senderId);
            return;
        }

        if (text != null) {
           if (text.equals(TelegramBotConstats.COMMAND_START)){
               sendMessage(chatId, TelegramBotConstats.MSG_GREETING);
               sendKeyBoard(chatId);
               log.info("юзер {}(Id: {}) использовал команду /start. ChatId: {}", senderUsername, senderId, chatId);
           } else if (text.equals(TelegramBotConstats.SEND_POST_TO_ADMIN)) {
               sendMessage(chatId, TelegramBotConstats.MSG_GREETING);
               log.info("юзер {}(Id: {}) отправил пост. ChatId: {}", senderUsername, senderId, chatId);
           } else if (text.equals(TelegramBotConstats.CHECK_ALL_POSTS) && adminIDs.contains(senderId)) {
               checkAllPendingPosts(chatId, text);
               log.info("Админ {}(Id: {}) посмотрел все посты. ChatId: {}", senderUsername, senderId, chatId);
           } else if (text.equals(TelegramBotConstats.PUBLISH_ALL_POSTS) && adminIDs.contains(senderId)){
               publishAllPosts(chatId);
               log.info("Админ {}(Id: {}) выложил все посты. ChatId: {}", senderUsername, senderId, chatId);
           } else {
               processNewSuggestion(chatId, text, senderId, senderUsername);
           }
       } else if (message.hasVideo()) {
           handleVideoMessage(chatId, message.getVideo(), message.getCaption(), senderId, senderUsername);
            log.info("юзер {}(Id: {}) отправил видео. ChatId: {}", senderUsername, senderId, chatId);
       } else if (message.hasPhoto()) {
           handlePhotoMessage(chatId, message.getPhoto(), message.getCaption(), senderId, senderUsername);
            log.info("юзер {}(Id: {}) отправил фото. ChatId: {}", senderUsername, senderId, chatId);
       } else {
           sendMessage(chatId, TelegramBotConstats.MSG_NOT_SUPPORTED);
            log.warn("юзер {}(Id: {}) отправил неподдерживаемое сообщение. ChatId: {}", senderUsername, senderId, chatId);
       }
    }

    private void sendKeyBoard(Long chatID){
        if (chatID == null) {
            log.error("Ошибка: chatID равен нулю");
            return;
        }
        try {
        SendMessage message = SendMessage
                .builder()
                .text("Выберете действие")
                .chatId(chatID.toString())
                .build();

        List<KeyboardRow> rows = List.of(new KeyboardRow(TelegramBotConstats.SEND_POST_TO_ADMIN));

        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup(rows);
        markup.setResizeKeyboard(true);
        message.setReplyMarkup(markup);

        telegramClient.execute(message);

        } catch (TelegramApiException e) {
            log.error("Ошибка клавиатуры: {}", e.getMessage());
        }
    }

    private void processNewSuggestion(Long chatId, String text, Long senderId, String senderUsername) {
        Post post = postService.savePostSuggestion(text, senderId, senderUsername,
                null, null, null, null, null, null, null, null, null, MediaType.TEXT);

        sendPostToAdmin(post);

        sendMessage(chatId, TelegramBotConstats.MSG_POST_SENT);
        log.info("Новый пост от: {}, текст: {}", senderId, text);
    }

    private void handleVideoMessage(Long chatId, Video video, String caption, Long senderId, String senderUsername) {
        String filePath = null;
        String fileURL = null;

        try {
            File file = telegramClient.execute(new GetFile(video.getFileId()));
            filePath = file.getFilePath();
            fileURL = file.getFileUrl(this.botToken);
        } catch (Exception e){
            log.error("Ошибка:{}", e.getMessage());
            return;
        }

        Post post = postService.savePostSuggestion(caption != null ? caption : "",
                senderId,
                senderUsername,
                video.getFileId(),
                filePath,
                video.getMimeType(),
                video.getFileSize(),
                caption != null ? caption : "",
                video.getDuration(),
                video.getWidth(),
                video.getHeight(),
                fileURL,
                MediaType.VIDEO
                );

        sendPostToAdmin(post);
        sendMessage(chatId, TelegramBotConstats.MSG_POST_SENT);
        log.info("Новый видео пост от: {}, fileId: {}", senderId, video.getFileId());
    }

    private void handlePhotoMessage(Long chatId, List<PhotoSize> photo, String caption, Long senderId, String senderUsername) {
        String filePath = null;
        String fileURL = null;

        PhotoSize photoSize = photo.stream().max(Comparator.comparingInt(PhotoSize::getFileSize)).orElse(null);

        if (photoSize == null) {
            log.error("Не удалось найти фото в списке PhotoSizes.");
            sendMessage(chatId, "Произошла ошибка при обработке вашего фото. Попробуйте еще раз.");
            return;
        }

        try {
            File file = telegramClient.execute(new GetFile(photoSize.getFileId()));
            if (photoSize != null) {
                filePath = file.getFilePath();
                fileURL = file.getFileUrl(this.botToken);
            } else {
                log.error("Полученный объект File для фото с ID {} равен null.", photoSize.getFileId());
                sendMessage(chatId, "Произошла ошибка при получении информации о вашем фото. Попробуйте еще раз.");
                return;
            }
        }catch (Exception e){
            log.error("Ошибка при получении file_path или file_url для фото с ID {}: {}", photoSize.getFileId(), e.getMessage());
            sendMessage(chatId, "Произошла ошибка при обработке вашего фото. Попробуйте еще раз.");
        }

        Post post = postService.savePostSuggestion(caption != null ? caption : "",
                senderId,
                senderUsername,
                photoSize.getFileId(),
                filePath,
                "image/jpeg",
                Long.valueOf(photoSize.getFileSize()),
                caption != null ? caption : "",
                null,
                photoSize.getWidth(),
                photoSize.getHeight(),
                fileURL,
                MediaType.IMAGE);
        sendPostToAdmin(post);
        sendMessage(chatId, TelegramBotConstats.MSG_POST_SENT);
        log.info("Новый фото пост от: {}, fileId: {}", senderId, photoSize.getFileId());
    }

    private void sendPostToAdmin(Post post) {

        var publishPost = InlineKeyboardButton.builder()
                .callbackData(TelegramBotConstats.CALLBACK_PUBLISH_PREFIX + post.getId())
                .text(TelegramBotConstats.APPROVED_POST)
                .build();

        var declinePost = InlineKeyboardButton.builder()
                .callbackData(TelegramBotConstats.CALLBACK_REJECT_PREFIX + post.getId())
                .text(TelegramBotConstats.REJECTED_POST)
                .build();

        var banUser = InlineKeyboardButton.builder()
                .callbackData(TelegramBotConstats.CALLBACK_BAN_PREFIX + post.getSenderId())
                .text(TelegramBotConstats.BAN_USER)
                .build();

        List<InlineKeyboardRow> inlineKeyboardRows = List.of(new InlineKeyboardRow(publishPost, declinePost, banUser));

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(inlineKeyboardRows);


        String postTextOrCaption = "";
        if (MediaType.TEXT.equals(post.getMediaType())) {
            postTextOrCaption = post.getContent();
        } else {
            postTextOrCaption = post.getCaption();
        }
        String fullAdminMessageText = String.format("📢 *Новый предложенный пост от* @%s (ID: `%d`)\n📅 %s\n\n%s",
                post.getSenderName(),
                post.getSenderId(),
                post.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                postTextOrCaption);

        log.info("Новый предложенный пост от: {}(ID: {}), текст: {}", post.getSenderName(), post.getSenderId(), post.getContent());

        for (Long adminID : adminIDs) {
            try {
               if (MediaType.VIDEO.equals(post.getMediaType())) {
                   SendVideo sendVideo = SendVideo
                           .builder()
                           .chatId(adminID)
                           .video(new InputFile(post.getFileID()))
                           .caption(fullAdminMessageText)
                           .parseMode(ParseMode.MARKDOWN)
                           .replyMarkup(inlineKeyboardMarkup)
                           .build();
                   Message sentMessage = telegramClient.execute(sendVideo);
                   postService.updateAdminMessageId(post.getId(), sentMessage.getMessageId());

               } else if(MediaType.IMAGE.equals(post.getMediaType())) {
                   SendPhoto sendPhoto = SendPhoto
                           .builder()
                           .chatId(adminID)
                           .photo(new InputFile(post.getFileID()))
                           .caption(fullAdminMessageText)
                           .parseMode(ParseMode.MARKDOWN)
                           .replyMarkup(inlineKeyboardMarkup)
                           .build();
                   Message sentMessage = telegramClient.execute(sendPhoto);
                   postService.updateAdminMessageId(post.getId(), sentMessage.getMessageId());
               } else {
                   SendMessage sendMessage = SendMessage
                           .builder()
                           .chatId(adminID)
                           .text(fullAdminMessageText)
                           .parseMode(ParseMode.MARKDOWN)
                           .replyMarkup(inlineKeyboardMarkup)
                           .build();
                  telegramClient.execute(sendMessage);
               }
            } catch (Exception e){
                log.error("Ошибка при отправке сообщения админу: {}", e.getMessage());
            }
        }
    }

    private void handleCallBackQuery(CallbackQuery callbackQuery) {
        Long chatID = callbackQuery.getMessage().getChatId();
        String callBackData = callbackQuery.getData();
        Integer messageId = callbackQuery.getMessage().getMessageId();

        Long realSenderID = callbackQuery.getFrom().getId();
        String senderUserName = callbackQuery.getFrom().getUserName();

        log.info("Получен callback chatID: {}, callBackData: '{}', realSenderID: {}",
                chatID, callBackData, realSenderID);

        if (!adminIDs.contains(realSenderID)) {
            sendMessage(chatID, TelegramBotConstats.MSG_NO_PERMISSIONS);
            log.warn("Попытка несанкционированного доступа от пользователя: {} (ID: {})",
                    senderUserName, realSenderID);
            return;
        }
            sendKeyboardAdminPanel(chatID);
        if (!adminIDs.contains(chatID)) {
            log.warn("Callback получен не из админского чата. ChatID: {}, SenderID: {}",
                    chatID, realSenderID);
            sendMessage(chatID, TelegramBotConstats.MSG_NO_PERMISSIONS);
            return;
        }

        if (callBackData == null || callBackData.trim().isEmpty()) {
            log.warn("Получен пустой callback data от админа: {}", realSenderID);
            sendMessage(chatID, "Ошибка: некорректные данные callback");
            return;
        }

        if (!rateLimitService.isCallBackAllowed(realSenderID, 3, 20)){
            log.warn("Дублирование callback от админа: {} (ID: {}), data: {}",
                    senderUserName, realSenderID, callBackData);
            sendMessage(chatID, TelegramBotConstats.CALLBACK_IS_STARTING);
            return;
        }

        try {
            if (callBackData.startsWith(TelegramBotConstats.CALLBACK_ACCEPT_APPEAL_PREFIX)){
                String senderIDStr = callBackData.replace(TelegramBotConstats.CALLBACK_ACCEPT_APPEAL_PREFIX, "");
                if (!isValidLong(senderIDStr)) {
                    log.warn("Некорректный ID пользователя({}) в callback: {}",TelegramBotConstats.CALLBACK_ACCEPT_APPEAL_PREFIX,senderIDStr);
                    sendMessage(chatID, "Ошибка: некорректный ID пользователя");
                    return;
                }
                Long senderID = Long.parseLong(senderIDStr);
                acceptAppeal(senderID);
                editMessageText(chatID, messageId, TelegramBotConstats.ADMIN_APPROVED_APPEAL, null);

            } else if (callBackData.startsWith(TelegramBotConstats.CALLBACK_REJECT_APPEAL_PREFIX)){
                String senderIDStr = callBackData.replace(TelegramBotConstats.CALLBACK_REJECT_APPEAL_PREFIX, "");
                if (!isValidLong(senderIDStr)) {
                    log.warn("Некорректный ID пользователя ({}) в callback: {}", TelegramBotConstats.CALLBACK_REJECT_APPEAL_PREFIX, senderIDStr);
                    sendMessage(chatID, "Ошибка: некорректный ID пользователя");
                    return;
                }
                Long senderID = Long.parseLong(senderIDStr);
                rejectAppeal(senderID);
                editMessageText(chatID, messageId, TelegramBotConstats.ADMIN_REJECTED_APPEAL, null);

            } else if (callBackData.startsWith(TelegramBotConstats.CALLBACK_PUBLISH_PREFIX)){
                String postIDStr = callBackData.replace(TelegramBotConstats.CALLBACK_PUBLISH_PREFIX, "");
                if (!isValidLong(postIDStr)) {
                    log.warn("Некорректный(publish) ID поста в callback: {}", postIDStr);
                    sendMessage(chatID, "Ошибка: некорректный ID поста");
                    return;
                }
                Long postID = Long.parseLong(postIDStr);
                publishPost(postID, chatID, messageId);
                acceptAppeal(realSenderID);
            } else if (callBackData.startsWith(TelegramBotConstats.CALLBACK_REJECT_PREFIX)){
                String postIDStr = callBackData.replace(TelegramBotConstats.CALLBACK_REJECT_PREFIX, "");
                if (!isValidLong(postIDStr)) {
                    log.warn("Некорректный(reject) ID поста в callback: {}", postIDStr);
                    sendMessage(chatID,"Ошибка: некорректный ID поста");
                    return;
                }
                Long postID = Long.parseLong(postIDStr);
                rejectedPost(postID, chatID, messageId);

            } else if (callBackData.startsWith(TelegramBotConstats.CALLBACK_BAN_PREFIX)){
                String senderIDStr = callBackData.replace(TelegramBotConstats.CALLBACK_BAN_PREFIX, "");
                if (!isValidLong(senderIDStr)) {
                    log.warn("Некорректный ID пользователя в callback: {}", senderIDStr);
                    sendMessage(chatID, "Ошибка: некорректный ID пользователя");
                    return;
                }
                Long senderID = Long.parseLong(senderIDStr);
                banService.banUser(senderID);
                editMessageText(chatID, messageId, TelegramBotConstats.USER_SUCCESSFULLY_BANNED, null);
                log.info("Админ {} заблокировал пользователя {}", realSenderID, senderID);

            } else {
                log.warn("Неизвестный callback data: {} от админа: {}", callBackData, realSenderID);
                sendMessage(chatID, "Ошибка: неизвестная команда");
            }
        } catch (Exception e) {
            log.error("Ошибка при обработке callback от админа {}: {}", realSenderID, e.getMessage(), e);
            sendMessage(chatID, "Произошла ошибка при обработке команды");
        }
    }

    private Boolean isValidLong(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Long.parseLong(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void sendKeyboardAdminPanel(Long chatId){
        SendMessage message = SendMessage
                .builder()
                .text("Меню")
                .chatId(chatId)
                .build();

        var showPendingPosts = KeyboardButton
                .builder()
                .text(TelegramBotConstats.CHECK_ALL_POSTS)
                .build();

        var publishAllPostsButton = KeyboardButton
                .builder()
                .text(TelegramBotConstats.PUBLISH_ALL_POSTS)
                .build();

        List<KeyboardRow> keyboardRow = List.of(new KeyboardRow(showPendingPosts, publishAllPostsButton));

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboardRow);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);
        replyKeyboardMarkup.setSelective(false);
        message.setReplyMarkup(replyKeyboardMarkup);

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка при отправке панели для администраторов. Ошибка: {}", e.getMessage(), e);
        }
    }

    private void checkAllPendingPosts(Long chatId, String text){
        if (adminIDs.contains(chatId)){

            if (text == null || text.isEmpty()) {
                log.error("Ошибка при просмотре постов в предложке: Текст пустой");
                return;
            }

            if (text.equals(TelegramBotConstats.CHECK_ALL_POSTS)){
                sendPendingPostsToAdmin(chatId);
            }
        }
    }

    private void sendBanMenuToUser(Long chatId) {
        SendMessage message = SendMessage
                .builder()
                .text(TelegramBotConstats.USER_HAS_BANNED)
                .chatId(chatId)
                .build();

        var banMessageMenuButton = KeyboardButton
                .builder()
                .text(TelegramBotConstats.USER_HAS_APPEAL)
                .build();

        List<KeyboardRow> keyboardRows = List.of(new KeyboardRow(banMessageMenuButton));

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboardRows);

        message.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        try {
            telegramClient.execute(message);
        } catch (Exception e) {
            log.error("Ошибка при отправке бан-сообщения: {} (chatId: {})", e.getMessage(), chatId);
        }
    }

    private void sendAppealMenu(String text, Long chatId, String senderUsername, Long senderId) {
        if (text.equals(TelegramBotConstats.USER_HAS_APPEAL)){
            try {
               BannedUsers bannedUsers = banService.isUserHasAppeal(senderId);
               if (bannedUsers != null && bannedUsers.getAppealBanUserStatus() == AppealBanUserStatus.PENDING){
                   sendMessage(chatId, TelegramBotConstats.APPEAL_PENDING_STATUS);
                   return;
               }

               sendMessage(chatId, "Введите вашу апелляцию");
                usersWritingAppeal.add(senderId);
                log.info("Забаненный юзер {}(Id: {}) начал подачу апелляции. ChatId: {}",
                        senderUsername, senderId, chatId);
            }catch (Exception e){
                log.error("Ошибка при отправке апелляции юзера: {} (chatId: {})", e.getMessage(), chatId);
            }
        } else if (usersWritingAppeal.contains(senderId)){
            try {
                BannedUsers bannedUser = banService.isUserHasAppeal(senderId);

                if (bannedUser == null){
                    bannedUser = new BannedUsers();
                    bannedUser.setSenderId(senderId);
                    bannedUser.setAppealBanUserStatus(AppealBanUserStatus.PENDING);
                    bannedUser.setAppealText(text);
                    bannedUsersRepository.save(bannedUser);
                } else {
                    bannedUser.setAppealBanUserStatus(AppealBanUserStatus.PENDING);
                    bannedUser.setAppealText(text);
                    bannedUsersRepository.save(bannedUser);
                }

                sendAppealToAdmins(senderId, senderUsername, text);

                sendMessage(chatId, TelegramBotConstats.APPEAL_HAS_SEND_TO_ADMIN);

                usersWritingAppeal.remove(senderId);

                log.info("Апелляция от юзера {}(Id: {}) отправлена админам. ChatId: {}",
                        senderUsername, senderId, chatId);

            }catch (Exception e){
                log.error("Ошибка при сохранении апелляции юзера: {} (chatId: {})", e.getMessage(), chatId);
                sendMessage(chatId, TelegramBotConstats.APPEAL_HAS_ERROR);
                usersWritingAppeal.remove(senderId);
            }
        }
    }

    public void acceptAppeal(Long senderId) {
        log.info("Одобрение апелляции для пользователя: {}", senderId);
        try {
            Optional<BannedUsers> appealUser = Optional.ofNullable(banService.isUserHasAppeal(senderId));

            if (appealUser.isPresent()) {;
                banService.deleteBanUser(senderId);
                log.info("Апелляция одобрена для пользователя: {}", senderId);
            }
        } catch (Exception e){
            log.warn("Попытка одобрить несуществующую апелляцию для пользователя: {}", senderId);
        }
    }

    public void rejectAppeal(Long senderId) {
        log.info("Отклонение апелляции для пользователя: {}", senderId);
        try {
            Optional<BannedUsers> appealUser = Optional.ofNullable(banService.isUserHasAppeal(senderId));

            if (appealUser.isPresent()) {
                BannedUsers appealEntity = appealUser.get();
                appealEntity.setAppealBanUserStatus(AppealBanUserStatus.REJECTED);
                bannedUsersRepository.save(appealEntity);
                log.info("Апелляция отклонена для пользователя: {}", senderId);
            }
        } catch (Exception e){
            log.warn("Попытка отклонить несуществующую апелляцию для пользователя: {}", senderId);
        }
    }

    private void sendAppealToAdmins(Long bannedUserId, String userName, String appealText) {

        var acceptAppealButton = InlineKeyboardButton.builder()
                .callbackData(TelegramBotConstats.CALLBACK_ACCEPT_APPEAL_PREFIX + bannedUserId)
                .text(TelegramBotConstats.APPEAL_HAS_APPROVED)
                .build();

        var rejectAppealButton = InlineKeyboardButton.builder()
                .callbackData(TelegramBotConstats.CALLBACK_REJECT_APPEAL_PREFIX + bannedUserId)
                .text(TelegramBotConstats.APPEAL_HAS_REJECTED)
                .build();

        List<InlineKeyboardRow> inlineKeyboardRows = List.of(
                new InlineKeyboardRow(acceptAppealButton, rejectAppealButton)
        );

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(inlineKeyboardRows);

        String adminMessageText = String.format(
                "🆘 *Новая апелляция на разбан*\n\n" +
                        "👤 Пользователь: @%s\n" +
                        "🆔 ID: `%d`\n" +
                        "📅 Дата апелляции: %s\n\n" +
                        "📝 *Текст апелляции:*\n%s",
                userName != null ? userName : "Неизвестно",
                bannedUserId,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                appealText != null ? appealText : "Пользователь просит разбан"
        );

        for (Long adminId : adminIDs) {
            try {
                SendMessage sendMessage = SendMessage.builder()
                        .chatId(adminId)
                        .text(adminMessageText)
                        .parseMode(ParseMode.MARKDOWN)
                        .replyMarkup(inlineKeyboardMarkup)
                        .build();

                telegramClient.execute(sendMessage);
                log.info("Апелляция успешно отправлена админу: {}", adminId);

            } catch (Exception e) {
                log.error("Ошибка при отправке апелляции админу {}: {}", adminId, e.getMessage());
            }
        }
        log.info("Апелляция от пользователя {} отправлена всем админам", bannedUserId);
    }

    private void publishAllPosts(Long chatId) {
        if (!adminIDs.contains(chatId)) {
            sendMessage(chatId, TelegramBotConstats.MSG_NO_PERMISSIONS);
            return;
        }

        List<Post> pendingPosts = postService.getPendingPosts();

        if (pendingPosts.isEmpty()) {
            sendMessage(chatId, TelegramBotConstats.MSG_NO_PENDING_POSTS);
            return;
        }

        int publishedCount = 0;
        for (Post post : pendingPosts) {
            try {
                if (MediaType.VIDEO.equals(post.getMediaType())){
                    SendVideo videoToChannel = SendVideo.builder()
                            .chatId(channelID)
                            .video(new InputFile(post.getFileID()))
                            .caption(post.getCaption())
                            .build();
                    telegramClient.execute(videoToChannel);
                } else if (MediaType.IMAGE.equals(post.getMediaType())){
                    SendPhoto photoToChannel = SendPhoto.builder()
                            .chatId(channelID)
                            .photo(new InputFile(post.getFileID()))
                            .caption(post.getCaption())
                            .parseMode(ParseMode.MARKDOWN)
                            .build();
                    telegramClient.execute(photoToChannel);
                } else {
                    SendMessage messageToChannel = SendMessage
                            .builder()
                            .chatId(channelID)
                            .text(post.getContent())
                            .parseMode(ParseMode.MARKDOWN)
                            .build();
                    telegramClient.execute(messageToChannel);
                }
                postService.markPostAsApproved(post.getId());
                publishedCount++;

            } catch (Exception e) {
                log.error("Ошибка при публикации поста {}: {}", post.getId(), e.getMessage());
            }
        }

        sendMessage(chatId, String.format("Опубликовано %d постов из %d", publishedCount, pendingPosts.size()));
    }

    private void rejectedPost(Long postId, Long chatID, Integer messageId) {
        Optional<Post> optionalPost = postService.getPostById(postId);


        Post post = optionalPost.get();

            if (post.getStatus() == PostStatus.REJECTED){
                editMessageText(chatID, messageId, TelegramBotConstats.MSG_POST_ALREADY_REJECTED, null);
                return;
            }

        String successMessage = String.format(
                "✅ *Пост успешно отклонен!*\n\n" +
                        "📝 От: @%s\n" +
                        "🆔 ID: @%s\n" +
                        "📅 Создан: %s\n" +
                        "🚀 Отклонен: %s",
                post.getSenderName(),
                post.getSenderId(),
                post.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );

            try {
                if (MediaType.TEXT.equals(post.getMediaType())) {
                    editMessageText(chatID, messageId, successMessage, null);
                } else {
                    editMessageCaption(chatID, messageId, successMessage, null);
                }
            }catch (Exception e){

            }
            postService.markPostAsRejected(postId);
    }

    private void publishPost(Long postId, Long chatID, Integer messageId) {
        Optional<Post> optionalPost = postService.getPostById(postId);

        if (optionalPost.isEmpty()) {
            log.error("Пост с ID {} не найден", postId);
            editMessageText(chatID, messageId, TelegramBotConstats.MSG_POST_NOT_FOUND, null);
            return;
        }

        Post post = optionalPost.get();

            if (post.getStatus() == PostStatus.APPROVED){
                editMessageText(chatID, messageId, TelegramBotConstats.MSG_POST_ALREADY_PUBLISHED, null);
                return;
            }

        try {
            if (MediaType.VIDEO.equals(post.getMediaType())){
                SendVideo videoToChannel = SendVideo.builder()
                        .chatId(channelID)
                        .video(new InputFile(post.getFileID()))
                        .caption(post.getCaption())
                        .build();
                telegramClient.execute(videoToChannel);
            } else if (MediaType.IMAGE.equals(post.getMediaType())){
                SendPhoto photoToChannel = SendPhoto.builder()
                        .chatId(channelID)
                        .photo(new InputFile(post.getFileID()))
                        .caption(post.getCaption())
                        .parseMode(ParseMode.MARKDOWN)
                        .build();
                telegramClient.execute(photoToChannel);
            } else {
                SendMessage message = SendMessage
                        .builder()
                        .chatId(channelID)
                        .text(post.getContent())
                        .parseMode(ParseMode.MARKDOWN)
                        .build();
                telegramClient.execute(message);
            }
        } catch (Exception e){
            log.error("Ошибка при редактировании сообщения {} в чате {}: {}", messageId, chatID, e.getMessage(), e);
        }
            String successMessage = String.format(
                        "✅ *Пост успешно опубликован!*\n\n" +
                                "📝 От: @%s\n" +
                                "🆔 ID: @%s\n" +
                                "📅 Создан: %s\n" +
                                "🚀 Опубликован: %s",
                        post.getSenderName(),
                        post.getSenderId(),
                        post.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                );
           try {
                if (MediaType.TEXT.equals(post.getMediaType())){
                    editMessageText(chatID, messageId, successMessage, null);
                } else {
                    editMessageCaption(chatID, messageId, successMessage, null);
                }
           } catch (Exception e){
               try {
                   if (MediaType.VIDEO.equals(post.getMediaType())){
                       editMessageCaption(chatID, messageId, TelegramBotConstats.MSG_HAS_ERROR, null);
                   } else {
                       editMessageText(chatID, messageId, TelegramBotConstats.MSG_HAS_ERROR, null);

                   }
               }catch (Exception e1){
                   log.error("Ошибка при обновлении сообщения админа об ошибке публикации: {}", e1.getMessage());
               }
           }
        postService.markPostAsApproved(postId);
        }

    private void sendPendingPostsToAdmin(Long chatId) {
    List<Post> posts = postService.getPendingPosts();

    if (posts.isEmpty()) {
        sendMessage(chatId, TelegramBotConstats.MSG_NO_PENDING_POSTS);
        log.info("Нету постов");
        return;
    }

    sendMessage(chatId, TelegramBotConstats.MSG_PENDING_POSTS_HEADER);
    for (Post post : posts) {
        var button1 = InlineKeyboardButton.builder()
                .text("Опубликовать")
                .callbackData(TelegramBotConstats.CALLBACK_PUBLISH_PREFIX + post.getId())
                .build();

        List<InlineKeyboardRow> inlineKeyboardRows = List.of(new InlineKeyboardRow(button1));
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup(inlineKeyboardRows);

        String postTextOrCaption = "";
        if (MediaType.TEXT.equals(post.getMediaType())) {
            postTextOrCaption = post.getContent();
        } else {
            postTextOrCaption = post.getCaption();
        }

        String fullAdminMessageText = String.format(
                "📢 *Пост на модерации от* @%s (ID: `%d`)\n📅 %s\n\n%s",
                post.getSenderName(),
                post.getSenderId(),
                post.getCreatedAt().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                postTextOrCaption
        );
       try {
           SendMessage sentMessage = SendMessage
                   .builder()
                   .chatId(chatId)
                   .text(fullAdminMessageText)
                   .parseMode(ParseMode.MARKDOWN)
                   .replyMarkup(inlineKeyboardMarkup)
                   .build();
           Message sentMessage1 = telegramClient.execute(sentMessage);
           postService.updateAdminMessageId(post.getId(), sentMessage1.getMessageId());
       } catch (Exception e){
        log.error("Ошибка при отправке неопубликованной новости админу {}: {}", chatId, e.getMessage(), e);
       }
    }
    }

    private void editMessageText(Long chatID, Integer messageID, String newmessage, InlineKeyboardMarkup inlineKeyboardMarkup) {
        try {
            EditMessageText editMessageText = EditMessageText
                    .builder()
                    .chatId(chatID)
                    .messageId(messageID)
                    .text(newmessage)
                    .replyMarkup(inlineKeyboardMarkup)
                    .parseMode(ParseMode.MARKDOWN)
                    .build();
            telegramClient.execute(editMessageText);
        } catch (Exception e) {
        log.error("Ошибка при редактировании сообщения {} в чате {}: {}", messageID, chatID, e.getMessage(), e);
        }
    }

    private void editMessageCaption(Long chatID, Integer messageID, String newCaption, InlineKeyboardMarkup inlineKeyboardMarkup) {
        try {
            EditMessageCaption editMessageCaption = EditMessageCaption
                    .builder()
                    .chatId(chatID)
                    .messageId(messageID)
                    .replyMarkup(inlineKeyboardMarkup)
                    .caption(newCaption)
                    .parseMode(ParseMode.MARKDOWN)
                    .build();
            telegramClient.execute(editMessageCaption);
        } catch (Exception e) {
            log.error("Ошибка при редактировании сообщения {} в чате {}: {}", messageID, chatID, e.getMessage(), e);
        }
    }

    private void sendMessage(Long chatId, String text) {
        try {
            SendMessage sendMessage = SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build();
            telegramClient.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("ошибка при отправке сообщения", e);
        }
    }
}
