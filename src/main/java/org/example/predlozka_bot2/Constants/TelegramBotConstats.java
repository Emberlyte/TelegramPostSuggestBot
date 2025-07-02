package org.example.predlozka_bot2.Constants;

public class TelegramBotConstats {

    public static final String CALLBACK_PUBLISH_PREFIX = "publish_";
    public static final String CALLBACK_REJECT_PREFIX = "reject_";
    public static final String CALLBACK_BAN_PREFIX = "ban_";

    public static final String COMMAND_START = "/start";
    public static final String COMMAND_ADMIN_PENDING = "/admin_pending";
    
    public static final String MSG_GREETING = "👋 Привет! Напишите свой пост для предложения в канал.";
    public static final String MSG_POST_SENT = "✅ Ваш пост отправлен на модерацию!";
    public static final String MSG_NOT_SUPPORTED = "❌ Данный тип сообщения не поддерживается";
    public static final String MSG_NO_PERMISSIONS = "❌ У вас нет прав для выполнения этого действия";
    public static final String MSG_NO_PENDING_POSTS = "📭 Нет постов на модерации";
    public static final String MSG_PENDING_POSTS_HEADER = "📋 Список постов на модерации:";
    public static final String MSG_POST_ALREADY_PUBLISHED = "✅ Этот пост уже был опубликован";
    public static final String MSG_POST_ALREADY_REJECTED = "❌ Этот пост уже был отклонен";
    public static final String MSG_POST_NOT_FOUND = "❌ Пост не найден";
    public static final String MSG_HAS_ERROR = "❌ Ошибка при публикации поста в канал";
    public static final String MSG_HAS_TO_MANY_REQUEST = "⚠️ Слишком много запросов! Подождите немного.";
    public static final String USER_SUCCESSFULLY_BANNED = "✅ Пользователь был успешно заблокирован";
    public static final String USER_HAS_BANNED = "❌ Вы были заблокированы в этом боте";
    public static final String CALLBACK_IS_STARTING = "⚠️ Действие уже выполняется. Подождите.";
}
