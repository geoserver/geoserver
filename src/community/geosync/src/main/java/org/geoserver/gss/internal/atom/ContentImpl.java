package org.geoserver.gss.internal.atom;

public class ContentImpl {

    /**
     * Intrinsic content value
     */
    private Object value;

    private String type;

    private String src;

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
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
     * @return the src
     */
    public String getSrc() {
        return src;
    }

    /**
     * @param src
     *            the src to set
     */
    public void setSrc(String src) {
        this.src = src;
    }

}
