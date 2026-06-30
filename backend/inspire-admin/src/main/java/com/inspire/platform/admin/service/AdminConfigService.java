package com.inspire.platform.admin.service;
import com.inspire.platform.admin.entity.AdminConfig;
import java.util.List;
public interface AdminConfigService {
    List<AdminConfig> getAll();
    void update(Integer id, String value);
    void push(String title, String content, String city);
}
