/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.template.editor.web;

import java.io.Serializable;
import java.util.List;
import org.geoserver.platform.resource.Paths;

public class TemplateResourceObject implements Serializable {
    /** serialVersionUID */
    private static final long serialVersionUID = 5135224484018193841L;

    private String filename, layername, workspacename;

    private String originalContent;

    private String content;

    private String dirty = ""; // marker telling if content has changed (needs saving)

    private String srcpath;

    private String destpath;

    private List<String> availablePaths;

    @Deprecated
    public TemplateResourceObject(
            String tpl, String source, String filename, String layername, String wsname) {
        this.originalContent = tpl;
        this.content = tpl;
        this.srcpath = source;
        this.filename = filename;
        this.layername = layername;
        this.workspacename = wsname;
    }

    public TemplateResourceObject(String tplName, String tplcontent, String sourcePath) {
        this.originalContent = tplcontent;
        this.content = tplcontent;
        this.srcpath = sourcePath;
        this.filename = tplName;
    }

    /**
     * Syncs with the templateResourceObject t content
     *
     * @param t : source templateObject
     */
    public void from(TemplateResourceObject t) {
        this.setFilename(t.getFilename());
        this.setAvailablePaths(t.getAvailablePaths());
        this.setSrcpath(t.getSrcpath());
        this.setDestpath(t.getDestpath());
        this.setContent(t.getContent());
        this.setOriginalContent(t.getOriginalContent());
    }

    public boolean isLocalPath() {
        return (srcpath.equals(destpath));
    }

    public String getSavePath() {
        return Paths.path(this.destpath, this.filename);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String content) {
        this.originalContent = content;
    }

    public void resetContent() {
        this.content = this.originalContent;
    }

    public String getDirty() {
        return dirty;
    }

    public boolean isDirty() {
        return !dirty.equalsIgnoreCase("");
    }

    public void setDirty(String dirty) {
        this.dirty = dirty;
    }

    public String getSrcpath() {
        return srcpath;
    }

    public List<String> getAvailablePaths() {
        return availablePaths;
    }

    public void setAvailablePaths(List<String> availablePaths) {
        this.availablePaths = availablePaths;
    }

    public String getDestpath() {
        return destpath;
    }

    public void setDestpath(String destpath) {
        this.destpath = destpath;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getLayername() {
        return layername;
    }

    public void setLayername(String layername) {
        this.layername = layername;
    }

    public String getWorkspacename() {
        return workspacename;
    }

    public void setWorkspacename(String workspacename) {
        this.workspacename = workspacename;
    }

    public void setSrcpath(String srcpath) {
        this.srcpath = srcpath;
    }
}
