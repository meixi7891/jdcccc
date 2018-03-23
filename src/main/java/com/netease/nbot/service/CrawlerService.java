package com.netease.nbot.service;

import com.netease.nbot.entity.SearchRequest;
import com.netease.nbot.entity.SearchResult;

import java.util.List;

public interface CrawlerService {

    List<SearchResult> search(SearchRequest requst) throws Exception;
}
