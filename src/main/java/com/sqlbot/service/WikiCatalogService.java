package com.sqlbot.service;

import com.sqlbot.dto.WikiCatalogNode;
import com.sqlbot.dto.WikiPageDTO;
import com.sqlbot.dto.WikiPageViewDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class WikiCatalogService {

    private static final String DEFAULT_PAGE = "pages/overview.md";

    private final WikiSearchService wikiSearchService;

    public WikiCatalogService(WikiSearchService wikiSearchService) {
        this.wikiSearchService = wikiSearchService;
    }

    public WikiCatalogNode getCatalogTree() {
        WikiCatalogNode root = new WikiCatalogNode();
        root.setName("知识库");
        root.setPath("");
        root.setPage(false);

        List<WikiPageDTO> pages = wikiSearchService.getAllPages();
        pages.sort(Comparator.comparing(WikiPageDTO::getRelativePath));

        for (WikiPageDTO page : pages) {
            insertPage(root, page);
        }

        sortTree(root);
        return root;
    }

    public WikiPageViewDTO getPageView(String relativePath) {
        WikiPageDTO page = wikiSearchService.getPageByPath(relativePath);
        if (page == null) {
            return null;
        }

        String body = wikiSearchService.getPageBody(relativePath);
        return new WikiPageViewDTO(
                page.getRelativePath(),
                page.getTitle(),
                body != null ? body : "",
                page.getType(),
                page.getDomain()
        );
    }

    public WikiPageViewDTO getDefaultPageView() {
        WikiPageViewDTO page = getPageView(DEFAULT_PAGE);
        if (page != null) {
            return page;
        }

        List<WikiPageDTO> pages = wikiSearchService.getAllPages();
        if (pages.isEmpty()) {
            return null;
        }

        WikiPageDTO first = pages.get(0);
        return getPageView(first.getRelativePath());
    }

    public int getPageCount() {
        return wikiSearchService.getPageCount();
    }

    private void insertPage(WikiCatalogNode root, WikiPageDTO page) {
        String[] segments = page.getRelativePath().split("/");
        WikiCatalogNode current = root;

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            boolean isLeaf = i == segments.length - 1;

            if (isLeaf) {
                WikiCatalogNode leaf = new WikiCatalogNode();
                leaf.setName(segment);
                leaf.setPath(page.getRelativePath());
                leaf.setTitle(page.getTitle());
                leaf.setPage(true);
                current.getChildren().add(leaf);
                return;
            }

            WikiCatalogNode child = findFolderChild(current, segment);
            if (child == null) {
                child = new WikiCatalogNode();
                child.setName(segment);
                child.setPath(buildFolderPath(segments, i));
                child.setPage(false);
                current.getChildren().add(child);
            }
            current = child;
        }
    }

    private WikiCatalogNode findFolderChild(WikiCatalogNode parent, String name) {
        for (WikiCatalogNode child : parent.getChildren()) {
            if (!child.isPage() && name.equals(child.getName())) {
                return child;
            }
        }
        return null;
    }

    private String buildFolderPath(String[] segments, int index) {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i <= index; i++) {
            if (i > 0) {
                path.append('/');
            }
            path.append(segments[i]);
        }
        return path.toString();
    }

    private void sortTree(WikiCatalogNode node) {
        node.getChildren().sort((a, b) -> {
            if (a.isPage() != b.isPage()) {
                return a.isPage() ? 1 : -1;
            }
            String left = a.isPage() ? a.getTitle() : a.getName();
            String right = b.isPage() ? b.getTitle() : b.getName();
            return left.compareToIgnoreCase(right);
        });

        for (WikiCatalogNode child : node.getChildren()) {
            if (!child.isPage()) {
                sortTree(child);
            }
        }
    }
}
