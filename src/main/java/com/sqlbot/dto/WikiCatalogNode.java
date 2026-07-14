package com.sqlbot.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class WikiCatalogNode {
    private String name;
    private String path;
    private String title;
    private boolean page;
    private List<WikiCatalogNode> children = new ArrayList<>();
}
