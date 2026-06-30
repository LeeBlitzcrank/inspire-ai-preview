package com.inspire.platform.admin.service;
import com.inspire.platform.admin.entity.AdminUserRow;
import java.util.List;
public interface AdminUserQueryService {
    List<AdminUserRow> search(String keyword, int page, int size);
    AdminUserRow detail(Long id);
    long total();
}
