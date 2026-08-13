package MikiMock.com.MikiMock.Common.constants;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static final String TOTAL_USERS =
            "analytics:total_users";

    public static final String PREMIUM_USERS =
            "analytics:premium_users";

    public static final String FREE_USERS =
            "analytics:free_users";

    public static final String TOTAL_INTERVIEWS =
            "analytics:total_interviews";

    public static final String ACTIVE_INTERVIEWS =
            "analytics:active_interviews";

    public static final String WAITING_USERS =
            "analytics:waiting_users";

    public static final String HEADER_EVENT_ID = "event-id";

    public static final String NOTIFICATION_PROCESSED =
            "notification:processed:";

}
