package com.briefai.document.parser.dto;

import java.util.List;

public class ParsedDocument {
    private List<ParsedPage> pages;
    private Integer pageCount;

    public ParsedDocument(List<ParsedPage> pages, Integer pageCount) {
        this.pages = pages;
        this.pageCount = pageCount;
    }

    public List<ParsedPage> getPages() {
        return pages;
    }

    public void setPages(List<ParsedPage> pages) {
        this.pages = pages;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }
}
