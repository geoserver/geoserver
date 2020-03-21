package com.boundlessgeo.gsr.api.map;

/**
 * Simple container for non-image ExportMap result
 */
public class ExportMap {
    private String href;

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public ExportMap(String href) {
        this.href = href;
    }
}
