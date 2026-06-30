package com.inspire.platform.ai.service;
import com.inspire.platform.ai.dto.*;

public interface AiService {
    AiGenerateResponse generate(AiGenerateRequest request);
    void select(AiSelectRequest request, Long userId);
    void publish(AiPublishRequest request, Long userId);
    AiExploreResponse explore(AiExploreRequest request);
}
