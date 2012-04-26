package org.geoserver.gss.internal.atom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.geoserver.bxml.atom.EntryEncoder;

/**
 * Atom feed document element as per <a href="http://tools.ietf.org/html/rfc4287">rfc4287</a>,
 * section 4.1.1.
 * 
 * @see EntryEncoder
 */
public class FeedImpl {

    public static final String NULL_ID = "00000000-0000-0000-0000-0000000006E0";

    private List<PersonImpl> author;

    private List<CategoryImpl> category;

    private List<PersonImpl> contributor;

    private GeneratorImpl generator;

    private String icon;

    private String id;

    private List<LinkImpl> link;

    // private XXX logo

    private String rights;

    private String subtitle;

    private String title;

    private Date updated;

    // private XXX extensionElement

    private Iterator<EntryImpl> entry;

    // custom "extensions"
    /**
     * Statement of what's the start position. Lower index is 1, not 0
     */
    private Long startPosition;

    /**
     * Not an statement of how many entries actually are on the feed, but of what's the maximum
     * number of entries
     */
    private Long maxEntries;

    /**
     * @return the author
     */
    @SuppressWarnings("unchecked")
    public List<PersonImpl> getAuthor() {
        if (author == null) {
            author = new ArrayList<PersonImpl>();
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
    @SuppressWarnings("unchecked")
    public List<CategoryImpl> getCategory() {
        if (category == null) {
            category = new ArrayList<CategoryImpl>();
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
     * @return the contributor
     */
    @SuppressWarnings("unchecked")
    public List<PersonImpl> getContributor() {
        if (contributor == null) {
            contributor = new ArrayList<PersonImpl>();
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
     * @return the generator
     */
    public GeneratorImpl getGenerator() {
        return generator;
    }

    /**
     * @param generator
     *            the generator to set
     */
    public void setGenerator(GeneratorImpl generator) {
        this.generator = generator;
    }

    /**
     * @return the icon URI
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @param icon
     *            icon URI
     */
    public void setIcon(String icon) {
        this.icon = icon;
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
    @SuppressWarnings("unchecked")
    public List<LinkImpl> getLink() {
        if (link == null) {
            link = new ArrayList<LinkImpl>();
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
     * @return the subtitle
     */
    public String getSubtitle() {
        return subtitle;
    }

    /**
     * @param subtitle
     *            the subtitle to set
     */
    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
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
     * @return the entry
     */
    @SuppressWarnings("unchecked")
    public Iterator<EntryImpl> getEntry() {
        return entry == null ? Collections.EMPTY_LIST.iterator() : entry;
    }

    /**
     * @param entry
     *            the entry to set
     */
    public void setEntry(Iterator<EntryImpl> entry) {
        this.entry = entry;
    }

    /**
     * Custom extension: statement of what's the start position. Lower index is 1, not 0
     * 
     * @return the startPosition, or {@code null} if not set
     */
    public Long getStartPosition() {
        return startPosition;
    }

    public void setStartPosition(Long startPosition) {
        this.startPosition = startPosition;
    }

    /**
     * Custom extension: Not an statement of how many entries actually are on the feed, but of
     * what's the maximum number of entries.
     * 
     * @return the maxEntries, or {@code null} if not set
     */
    public Long getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(Long maxEntries) {
        this.maxEntries = maxEntries;
    }

}
