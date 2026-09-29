package com.briefai.document.parser.dto;

public class ParsedPage {
    private Integer pageNumber;
    private String text;

    public ParsedPage(Integer pageNumber, String text) {
        this.pageNumber = pageNumber;
        this.text = text;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
