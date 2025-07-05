package org.example.predlozka_bot2.Constants;

public class TelegramBotConstats {

    public static final String CALLBACK_PUBLISH_PREFIX = "publish_";
    public static final String CALLBACK_REJECT_PREFIX = "reject_";
    public static final String CALLBACK_BAN_PREFIX = "ban_";
    public static final String CALLBACK_REJECT_APPEAL_PREFIX =  "reject_appeal_";
    public static final String CALLBACK_ACCEPT_APPEAL_PREFIX =  "reject_appeal_";

    public static final String COMMAND_START = "/start";
    
    public static final String MSG_GREETING = "👋 Привет! Напишите свой пост для предложения в канал.";
    public static final String MSG_POST_SENT = "✅ Ваш пост отправлен на модерацию!";
    public static final String APPROVED_POST = "✅ Опубликовать";
    public static final String REJECTED_POST = "❌ Отклонить";
    public static final String BAN_USER = "🚫 заблокировать пользователя";
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
    public static final String USER_HAS_APPEAL = "⚖️ Поддать апелляцию";
    public static final String APPEAL_HAS_APPROVED = "✅ Принять апелляцию";
    public static final String APPEAL_HAS_REJECTED = "❌ Отклонить апелляцию";
    public static final String APPEAL_HAS_ERROR = "❌ Произошла ошибка при отправке апелляции. Попробуйте еще раз.";
    public static final String APPEAL_HAS_SEND_TO_ADMIN = "✅ Ваша апелляция отправлена админам.";
    public static final String APPEAL_PENDING_STATUS = "⏳ Ваша апелляция уже находится на рассмотрении.";
    public static final String ADMIN_APPROVED_APPEAL = "✅ Апелляция одобрена. Пользователь разбанен.";
    public static final String ADMIN_REJECTED_APPEAL = "❌ Апелляция не одобрена. Пользователь не разбанен.";
    public static final String CALLBACK_IS_STARTING = "⚠️ Действие уже выполняется. Подождите.";
    public static final String CHECK_ALL_POSTS = "👁️ Посмотреть все посты";
    public static final String PUBLISH_ALL_POSTS = "✅ Выложить все посты";
    public static final String SEND_POST_TO_ADMIN = "📤 Отправить пост";
}
