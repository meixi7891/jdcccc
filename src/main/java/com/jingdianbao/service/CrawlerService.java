package com.jingdianbao.service;

import com.jingdianbao.entity.SearchRequest;
import com.jingdianbao.entity.SearchResult;

import java.util.List;

public interface CrawlerService {

    List<SearchResult> search(SearchRequest requst) throws Exception;
}
