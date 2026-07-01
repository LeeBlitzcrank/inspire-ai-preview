package com.inspire.platform.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("ai_call_log")
public class AiCallLog {
    private Long id;
    private LocalDate callDate;
    private String keyword;
    private Long userId;
}
