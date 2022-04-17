package com.nowcoder.util;

public interface CommunityConstant {

    int ACTIVATION_SUCCESS = 0;

    int ACTIVATION_REPEAT = 1;

    int ACTIVATION_FAILURE = 2;

    long DEFAULT_EXPIRED_SECONDS = 3600 * 12;

    //这里牛客教程中有个小错误,教程代码规定的是int类型,当未来时间间隔超过24天后,int类型就会溢出,导致未来过期时间不是规定的记住我的较长过期日期,
    long REMEMBER_EXPIRED_SECONDS = 3600 * 24 * 100;

    int ENTITY_TYPE_POST = 1;

    int ENTITY_TYPE_COMMENT = 2;

    int ENTITY_TYPE_USER = 3;

    String TOPIC_COMMENT = "comment";

    String TOPIC_LIKE = "like";

    String TOPIC_FOLLOW = "follow";

    String TOPIC_PUBLISH = "publish";

    String TOPIC_DELETE = "delete";

    String TOPIC_SHARE = "share";

    int SYSTEM_USER_ID = 1;

    String AUTHORITY_USER = "user";

    String AUTHORITY_ADMIN = "admin";

    String AUTHORITY_MODERATOR = "moderator";

}
