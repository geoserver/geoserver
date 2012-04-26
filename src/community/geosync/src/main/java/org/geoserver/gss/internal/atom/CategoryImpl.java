package org.geoserver.gss.internal.atom;

public class CategoryImpl {

    private String term;

    private String scheme;

    public String getTerm() {
        return term;
    }

    public String getScheme() {
        return scheme;
    }

    /**
     * @param term
     *            the term to set
     */
    public void setTerm(String term) {
        this.term = term;
    }

    /**
     * @param scheme
     *            the scheme to set
     */
    public void setScheme(String scheme) {
        this.scheme = scheme;
    }

}
