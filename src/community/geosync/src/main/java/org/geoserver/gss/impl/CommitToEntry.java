package org.geoserver.gss.impl;

import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.DiffEntry;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.repository.SpatialOps;
import org.geoserver.gss.internal.atom.CategoryImpl;
import org.geoserver.gss.internal.atom.ContentImpl;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.LinkImpl;
import org.geoserver.gss.internal.atom.PersonImpl;
import org.opengis.geometry.BoundingBox;

import com.google.common.base.Function;

/**
 * Adapts {@link RevCommit} to an Atom {@link EntryImpl}.
 * <p>
 * Populates the {@link EntryImpl} mapping GSS concepts (from Resolution Feed Encoding, section
 * 9.2.3.2) to GeoGit's.
 * </p>
 */
class CommitToEntry implements Function<RevCommit, EntryImpl> {

    private final GeoGIT geoGit;

    public CommitToEntry(final GeoGIT geoGit) {
        this.geoGit = geoGit;
    }

    @Override
    public EntryImpl apply(final RevCommit commit) {

        EntryImpl atomEntry = new EntryImpl();

        // NOTE: this is not really what the atom:entry should be, as if someone requested an entry
        // by it this wouldn't indicate whether it's a feature insert,update,or delete. But this is
        // a concept of GSS exclusively, as we can't use the commit id to refer to a single feature
        // change neither, so the mapping from entry id to DiffEntry should be in the GSS database,
        // and a new atom:entry id should be automatically generated as stated in the spec
        ObjectId objectId = commit.getId();

        atomEntry.setId(objectId.toString());// TODO: convert to UUID

        atomEntry.setAuthor(author(commit));
        atomEntry.setCategory(category(commit));
        atomEntry.setContent(content(commit));
        atomEntry.setLink(link(commit));
        atomEntry.setTitle(title(commit));
        atomEntry.setUpdated(updated(commit));

        long t = System.currentTimeMillis();
        Object where = where(commit);
        atomEntry.setWhere(where);
        t = System.currentTimeMillis() - t;
        System.out.println(getClass().getSimpleName() + ": Computing where of " + commit + " took "
                + t + "ms. " + where);

        atomEntry.setContributor(contributor(commit));
        // atomEntry.setPublished(published);
        // atomEntry.setRights(rights);
        // atomEntry.setSource(source);

        return atomEntry;
    }

    /**
     * <p>
     * The date the change was made. All time shall be zulu time.
     * </p>
     */
    private Date updated(final RevCommit commit) {
        /*
         * No need to perform any timezone conversion. Dates are always in GMT and presenting the
         * timestamp in 'zulu' time is an encoding issue anyways
         */
        return new Date(commit.getTimestamp());
    }

    /**
     * <p>
     * The value of the title element shall be 'Proposal Accepted' or 'Proposal Rejected' depending
     * of the disposition of the originating change proposal.
     * </p>
     */
    private String title(RevCommit commit) {
        return "Proposal Accepted";
    }

    /**
     * <p>
     * A link with the value of the "rel" attribute set to "disposition" and the value of the href
     * attribute set to point to the change feed entry that has been accepted or rejected.
     * </p>
     */
    private List<LinkImpl> link(RevCommit commit) {
        LinkImpl link = new LinkImpl();
        link.setRel("disposition");
        link.setHref("http://do.no.forget/to/point/to/the/changefeed/entry");
        return Collections.singletonList(link);
    }

    /**
     * <p>
     * There shall be at one category element in the entry indicating the disposition of the
     * originating change proposal. The "term" attribute shall have a value of "Accepted" if the
     * originating change proposal was accepted or "Rejected" if the originating change proposal was
     * rejected. The scheme attribute shall have the value:
     * "http://www.opengis.org/geosync/resolutions"
     * </p>
     */
    private List<CategoryImpl> category(RevCommit commit) {
        CategoryImpl cat = new CategoryImpl();
        cat.setTerm("Accepted");
        cat.setScheme("http://www.opengis.org/geosync/resolutions");
        return Collections.singletonList(cat);
    }

    /**
     * <p>
     * If the originating change proposal was rejected, the georss:where element shall be omitted.
     * If the originating change proposal was accepted, the georss:where element shall have a value
     * according to the following rules: If the modified feature is non-spatial then the element
     * shall not be specified. If the modified feature is spatial and includes a single point
     * geometry then that geometry shall be used as the value of element. If the modified feature is
     * spatial and includes a non-single point geometry then a gml:Envelope containing the geometry
     * shall be used as the value of the <georss:where>element.
     */
    private Object where(final RevCommit commit) {
        final ObjectId parentId = commit.getParentIds().get(0);
        BoundingBox where = null;
        Iterator<DiffEntry> diff;
        try {
            diff = geoGit.diff().setOldVersion(parentId).setNewVersion(commit.getId()).call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        DiffEntry next;
        BoundingBox diffEnv;
        while (diff.hasNext()) {
            next = diff.next();
            diffEnv = next.getWhere();
            if (diffEnv != null && where == null) {
                where = SpatialOps.expandToInclude(where, diffEnv);
            }
        }
        if (where != null && where.getSpan(0) == 0D && where.getSpan(1) == 0D) {
            // it's a single point
            return SpatialOps.toGeometry(where);
        }
        return where;
    }

    /**
     * <p>
     * If the change proposal was rejected then the content element may contain a narrative
     * describing why it was rejected. If the change proposal was accepted but modified then the
     * content element may contain a narrative describing the modifications
     * </p>
     */
    private ContentImpl content(RevCommit commit) {
        ContentImpl content = new ContentImpl();
        content.setType("text/plain");
        content.setValue(commit.getMessage());
        return content;
    }

    /**
     * @param commit
     * @return committer
     */
    private List<PersonImpl> contributor(final RevCommit commit) {
        PersonImpl contributor = new PersonImpl();
        contributor.setName(commit.getCommitter());
        return Collections.singletonList(contributor);
    }

    /**
     * @param commit
     * @return commit author
     */
    private List<PersonImpl> author(final RevCommit commit) {
        PersonImpl author = new PersonImpl();
        author.setName(commit.getAuthor());
        return Collections.singletonList(author);
    }

}
