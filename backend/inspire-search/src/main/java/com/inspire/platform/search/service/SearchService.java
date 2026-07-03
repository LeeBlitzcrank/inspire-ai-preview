package com.inspire.platform.search.service;
import com.inspire.platform.search.dto.SearchResultVO;
import java.util.List;
public interface SearchService {
    List<SearchResultVO> search(String keyword, String tag, int page, int size, String searchAfter);
}
