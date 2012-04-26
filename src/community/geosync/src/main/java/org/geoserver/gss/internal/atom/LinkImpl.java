package org.geoserver.gss.internal.atom;

public class LinkImpl {

    private String href;

    private String rel;

    private String type;

    private String hreflang;

    private String title;

    private Long length;

    /**
     * @return the href
     */
    public String getHref() {
        return href;
    }

    /**
     * @param href
     *            the href to set
     */
    public void setHref(String href) {
        this.href = href;
    }

    /**
     * @return the rel
     */
    public String getRel() {
        return rel;
    }

    /**
     * @param rel
     *            the rel to set
     */
    public void setRel(String rel) {
        this.rel = rel;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type
     *            the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the hreflang
     */
    public String getHreflang() {
        return hreflang;
    }

    /**
     * @param hreflang
     *            the hreflang to set
     */
    public void setHreflang(String hreflang) {
        this.hreflang = hreflang;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title
     *            the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the length
     */
    public Long getLength() {
        return length;
    }

    /**
     * @param length
     *            the length to set
     */
    public void setLength(Long length) {
        this.length = length;
    }

}
