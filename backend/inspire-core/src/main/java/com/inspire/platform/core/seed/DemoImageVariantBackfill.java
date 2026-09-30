/**
 * 文件：backend/inspire-core/src/main/java/com/inspire/platform/core/seed/DemoImageVariantBackfill.java
 * 所属模块：核心业务模块，负责灵感、评论、收藏、消息、系列、文件和通知
 * 主要职责：工程源码或配置文件
 * 维护说明：注释解释文件边界和核心意图，具体业务规则以方法、组件和主文档说明为准。
 * INSPIRE_FILE_HEADER
 */
package com.inspire.platform.core.seed;

import com.inspire.platform.core.service.ImageVariantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 一次性为已有 60 张种子图补生成 WebP 三档变体，并更新数据库引用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "inspire.image.backfill-variants", havingValue = "true")
public class DemoImageVariantBackfill implements ApplicationRunner {

    private static final int DEMO_IMAGE_COUNT = 60;
    private final ImageVariantService imageVariantService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int updated = 0;
        for (int i = 0; i < DEMO_IMAGE_COUNT; i++) {
            String sourceKey = "upload/demo/" + i + ".jpg";
            Map<Integer, String> variants = imageVariantService.ensureMinioVariants(sourceKey);
            String newUrl = variants.get(800);
            if (newUrl == null) continue;

            String cdnV2 = "https://img.20sherry.com/upload/demo/" + i + ".jpg?v=2";
            String cdnV1 = "https://img.20sherry.com/upload/demo/" + i + ".jpg";
            String apiUrl = "/api/file/view?key=upload/demo/" + i + ".jpg";

            updated += jdbcTemplate.update(
                    "UPDATE inspire_main SET img = ? WHERE img IN (?, ?, ?)",
                    newUrl, cdnV2, cdnV1, apiUrl);
            updated += jdbcTemplate.update(
                    "UPDATE inspire_main SET images = "
                            + "REPLACE(REPLACE(REPLACE(images, ?, ?), ?, ?), ?, ?) "
                            + "WHERE images LIKE ? OR images LIKE ? OR images LIKE ?",
                    cdnV2, newUrl, cdnV1, newUrl, apiUrl, newUrl,
                    "%" + cdnV2 + "%", "%" + cdnV1 + "%", "%" + apiUrl + "%");
        }
        log.info("[VariantBackfill] WebP变体回填完成，数据库更新行数={}", updated);
    }
}
