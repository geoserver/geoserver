package org.geoserver.gss.internal.atom;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Atom {@code entry} element as per <a href="http://tools.ietf.org/html/rfc4287">rfc4287</a>,
 * section 4.1.1.
 * 
 */
public class EntryImpl {

    private List<PersonImpl> author;

    private List<CategoryImpl> category;

    private ContentImpl content;

    private List<PersonImpl> contributor;

    private String id;

    private List<LinkImpl> link;

    private Date published;

    private String rights;

    private String source;

    private String summary;

    private String title;

    private Date updated;

    // private XXX extensionElement*

    /**
     * The georss:where element, usually a bounding box, a point, or a polygon
     */
    private Object where;

    /**
     * @return the author
     */
    public List<PersonImpl> getAuthor() {
        if (author == null) {
            author = new ArrayList<PersonImpl>(2);
        }
        return author;
    }

    /**
     * @param author
     *            the author to set
     */
    public void setAuthor(List<PersonImpl> author) {
        this.author = author;
    }

    /**
     * @return the category
     */
    public List<CategoryImpl> getCategory() {
        if (category == null) {
            category = new ArrayList<CategoryImpl>(2);
        }
        return category;
    }

    /**
     * @param category
     *            the category to set
     */
    public void setCategory(List<CategoryImpl> category) {
        this.category = category;
    }

    /**
     * @return the content
     */
    public ContentImpl getContent() {
        return content;
    }

    /**
     * @param content
     *            the content to set
     */
    public void setContent(ContentImpl content) {
        this.content = content;
    }

    /**
     * @return the contributor
     */
    public List<PersonImpl> getContributor() {
        if (contributor == null) {
            contributor = new ArrayList<PersonImpl>(2);
        }
        return contributor;
    }

    /**
     * @param contributor
     *            the contributor to set
     */
    public void setContributor(List<PersonImpl> contributor) {
        this.contributor = contributor;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return the link
     */
    public List<LinkImpl> getLink() {
        if (link == null) {
            link = new ArrayList<LinkImpl>(2);
        }
        return link;
    }

    /**
     * @param link
     *            the link to set
     */
    public void setLink(List<LinkImpl> link) {
        this.link = link;
    }

    /**
     * @return the published
     */
    public Date getPublished() {
        return published;
    }

    /**
     * @param published
     *            the published to set
     */
    public void setPublished(Date published) {
        this.published = published;
    }

    /**
     * @return the rights
     */
    public String getRights() {
        return rights;
    }

    /**
     * @param rights
     *            the rights to set
     */
    public void setRights(String rights) {
        this.rights = rights;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source
     *            the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * @param summary
     *            the summary to set
     */
    public void setSummary(String summary) {
        this.summary = summary;
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
     * @return the updated
     */
    public Date getUpdated() {
        return updated;
    }

    /**
     * @param updated
     *            the updated to set
     */
    public void setUpdated(Date updated) {
        this.updated = updated;
    }

    /**
     * @return the {@code georss:where} object, usually a bounding box, point, or polygon
     */
    public Object getWhere() {
        return where;
    }

    /**
     * @param where
     *            the {@code georss:where} object, usually a bounding box, point, or polygon
     */
    public void setWhere(Object where) {
        this.where = where;
    }

}
