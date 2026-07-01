package com.inspire.platform.admin.service;
import com.inspire.platform.admin.dto.InspireAdminListVO;
import java.util.List;
import java.util.Map;
public interface AdminInspireService {
    Map<String, Object> list(String keyword, String tag, Integer status, int page, int size);
    void block(Long id);
    void unblock(Long id);
    void approve(Long id);
    void reject(Long id);
    Map<String, Object> listPending(int page, int size);
}
