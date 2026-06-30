package com.sqlbot.service;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WikiSearchResult {

    private String relativePath;
    private String title;
    private int score;
    private List<String> snippets = new ArrayList<>();
}
