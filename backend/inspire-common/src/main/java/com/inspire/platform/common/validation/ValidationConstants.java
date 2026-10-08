/**
 * 文件：backend/inspire-common/src/main/java/com/inspire/platform/common/validation/ValidationConstants.java
 * 所属模块：公共基础模块，提供统一响应、异常、鉴权上下文和通用工具
 * 主要职责：定义跨模块共享的输入边界，保证 DTO、业务层和前端提示使用同一组数值
 * 维护说明：新增业务字段时应先在这里补充边界，再在对应 DTO 和服务层接入。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.common.validation;

import com.inspire.platform.common.util.TitleUtil;

/**
 * 生产输入边界常量。
 *
 * <p>注解校验负责在进入业务前给出快速、统一的错误；服务层仍会再次校验，
 * 因为内部调用、消息重放和任务恢复可能绕过 Controller。</p>
 */
public final class ValidationConstants {

    public static final int USERNAME_MIN = 4;
    public static final int USERNAME_MAX = 20;
    public static final String USERNAME_PATTERN = "[A-Za-z0-9_]{4,20}";

    public static final int NICKNAME_MIN = 2;
    public static final int NICKNAME_MAX = 20;

    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 64;
    public static final int LOGIN_PASSWORD_MAX = 128;

    public static final int EMAIL_MAX = 254;
    public static final int PHONE_LENGTH = 11;
    public static final String PHONE_PATTERN = "1[3-9]\\d{9}";
    public static final int AVATAR_MAX = 2048;
    public static final int CITY_MAX = 32;

    public static final int TITLE_MAX = TitleUtil.MAX_LENGTH;
    public static final int CONTENT_MAX = 20_000;
    public static final int TAG_MAX = 20;
    public static final int MEDIA_URL_MAX = 2048;
    public static final int IMAGE_JSON_MAX = 20_000;
    public static final int MAX_IMAGES = 9;

    public static final int COMMENT_MAX = 500;
    public static final int MESSAGE_MAX = 1000;
    public static final int MESSAGE_EXTRA_JSON_MAX = 4_000;
    public static final int MESSAGE_TYPE_MAX = 20;

    public static final int AI_KEYWORD_MAX = 100;
    public static final int AI_PATH_MAX = 1000;
    public static final int AI_VARIANTS_MAX = 5;

    public static final int WORLD_SOURCE_TITLE_MAX = 120;
    public static final int WORLD_SOURCE_AUTHOR_MAX = 80;
    public static final int WORLD_SOURCE_TEXT_MAX = 6_000;
    public static final int WORLD_GUIDANCE_MAX = 500;
    public static final int WORLD_CHOICE_KEY_MAX = 40;
    public static final int WORLD_CHOICE_TEXT_MAX = 160;

    public static final int PAGE_MIN = 1;
    public static final int PAGE_SIZE_MAX = 50;

    private ValidationConstants() {
    }
}
