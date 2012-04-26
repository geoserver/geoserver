package org.geoserver.gss.impl;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.xml.namespace.QName;

import net.opengis.wfs.DeleteElementType;
import net.opengis.wfs.InsertElementType;
import net.opengis.wfs.PropertyType;
import net.opengis.wfs.UpdateElementType;
import net.opengis.wfs.WfsFactory;
import net.opengis.wfs.impl.WfsFactoryImpl;

import org.geogit.api.DiffEntry;
import org.geogit.api.DiffEntry.ChangeType;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.RevCommit;
import org.geogit.repository.Repository;
import org.geoserver.gss.internal.atom.ContentImpl;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.PersonImpl;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.springframework.util.Assert;

import com.google.common.base.Function;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * Adapts a change to a single {@code Feature} given by a {@link DiffEntry} to an Atom
 * {@link EntryImpl}.
 * 
 */
class DiffToEntry implements Function<DiffEntry, EntryImpl> {

    private static FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools
            .getDefaultHints());

    private static final GeometryFactory geometryFactory = new GeometryFactory();

    private final GSS gss;

    /**
     * Set by {@link #content()}, to be used by {@link #where()}
     */
    private Object currGeorssWhereValue;

    public DiffToEntry(final GSS gss) {
        this.gss = gss;
    }

    /**
     * @see com.google.common.base.Function#apply(java.lang.Object)
     */
    public EntryImpl apply(final DiffEntry diffEnty) {
        final ChangeType changeType = diffEnty.getType();
        // commit is always the diffEntry newCommitId, as newCommitId doesn't mean it's temporaly
        // newest, just at the "new" end of the diff, and may be the older or the newer commit
        // depending on which direction the diff was performed
        Repository repository = gss.getGeoGit().getRepository();
        ObjectId newCommitId = diffEnty.getNewCommitId();
        // so just keep in mind it could be null only in the case it points to the empty repository
        // NULL revision (well, at least until we figure out diffing uncommitted changes in the
        // index?)
        final RevCommit newCommit;
        if (newCommitId.isNull()) {
            // fake it
            newCommit = new RevCommit(ObjectId.NULL);
            newCommit.setMessage("Empty repository");
            newCommit.setTreeId(ObjectId.NULL);
        } else {
            newCommit = repository.getCommit(newCommitId);
        }

        EntryImpl atomEntry = new EntryImpl();

        final String atomEntryId = UUIDUtil.buildReplicationEntryUUID(diffEnty);

        atomEntry.setId(atomEntryId);
        atomEntry.setTitle(title(diffEnty));
        atomEntry.setSummary(newCommit.getMessage());
        atomEntry.setUpdated(new Date(newCommit.getTimestamp()));
        atomEntry.getAuthor().addAll(author(newCommit));
        atomEntry.getContributor().addAll(contributor(newCommit));
        atomEntry.setContent(content(diffEnty));
        atomEntry.setWhere(where());

        // atomEntry.setCategory(category);
        // atomEntry.setLink(link);
        // atomEntry.setPublished(published);
        // atomEntry.setRights(rights);
        // atomEntry.setSource(source);

        return atomEntry;
    }

    private Object where() {
        if (currGeorssWhereValue == null) {
            return null;
        }
        return currGeorssWhereValue;
    }

    private ContentImpl content(final DiffEntry diff) {
        final String namespace = diff.getPath().get(0);
        final String typeName = diff.getPath().get(1);
        final QName featureTypeName = new QName(namespace, typeName);
        final String featureId = diff.getPath().get(2);

        final FeatureType featureType = gss.getFeatureType(namespace, typeName);
        final GeoGIT ggit = gss.getGeoGit();
        Object contentValue;
        final Repository repository = ggit.getRepository();
        switch (diff.getType()) {
        case ADD: {
            ObjectId contentId = diff.getNewObjectId();
            Feature addedFeature = repository.getFeature(featureType, featureId, contentId);
            setCurrGeorssWhereValue(null, addedFeature.getBounds());
            contentValue = insert(featureTypeName, addedFeature);
            break;
        }
        case DELETE: {
            ObjectId oldStateId = diff.getOldObjectId();
            Feature oldState = repository.getFeature(featureType, featureId, oldStateId);
            setCurrGeorssWhereValue(oldState.getBounds(), null);
            contentValue = delete(featureTypeName, featureId);
            break;
        }
        case MODIFY: {

            ObjectId oldStateId = diff.getOldObjectId();
            ObjectId newStateId = diff.getNewObjectId();

            Feature oldState = repository.getFeature(featureType, featureId, oldStateId);
            Feature newState = repository.getFeature(featureType, featureId, newStateId);

            setCurrGeorssWhereValue(oldState.getBounds(), newState.getBounds());

            contentValue = update(featureTypeName, oldState, newState);
            break;
        }
        default:
            throw new IllegalStateException();
        }

        ContentImpl content = new ContentImpl();
        content.setValue(contentValue);
        return content;
    }

    private void setCurrGeorssWhereValue(BoundingBox oldBounds, BoundingBox newBounds) {
        BoundingBox aggregated;
        if (oldBounds == null || oldBounds.isEmpty()) {
            aggregated = newBounds;
        } else if (newBounds == null || newBounds.isEmpty()) {
            aggregated = oldBounds;
        } else if (oldBounds.equals(newBounds)) {
            aggregated = newBounds;
        } else {
            // it's an update and neither is empty
            aggregated = new ReferencedEnvelope(newBounds);
            CoordinateReferenceSystem newCrs = newBounds.getCoordinateReferenceSystem();
            CoordinateReferenceSystem oldCrs = oldBounds.getCoordinateReferenceSystem();
            Assert.notNull(newCrs, "newCrs is null");
            Assert.notNull(oldCrs, "oldCrs is null");
            if (!newCrs.equals(oldCrs)) {
                try {
                    oldBounds = (BoundingBox) CRS.transform(oldBounds, newCrs);
                } catch (TransformException e) {
                    throw new RuntimeException(e);
                }
            }
            ((ReferencedEnvelope) aggregated).expandToInclude((ReferencedEnvelope) oldBounds);
        }

        boolean isPoint = aggregated.getSpan(0) == 0D && aggregated.getSpan(1) == 0D;
        if (isPoint) {
            Point point = geometryFactory.createPoint(new Coordinate(aggregated.getMinX(),
                    aggregated.getMinY()));
            point.setUserData(aggregated.getCoordinateReferenceSystem());
            this.currGeorssWhereValue = point;

        } else {
            this.currGeorssWhereValue = aggregated;
        }
    }

    @SuppressWarnings("unchecked")
    private UpdateElementType update(final QName featureTypeName, final Feature oldState,
            final Feature newState) {

        final FeatureDiff diff;
        diff = new SimpleFeatureDiff((SimpleFeature) oldState, (SimpleFeature) newState);

        final WfsFactory fac = WfsFactoryImpl.eINSTANCE;
        UpdateElementType update = fac.createUpdateElementType();
        update.setTypeName(featureTypeName);

        QName name;
        Object value;
        for (Property prop : diff.getNewValues()) {

            {
                Name propName = prop.getName();
                String nsUri = propName.getNamespaceURI() == null ? featureTypeName
                        .getNamespaceURI() : propName.getNamespaceURI();
                String localPropName = propName.getLocalPart();
                name = new QName(nsUri, localPropName);
                value = prop.getValue();
            }

            PropertyType wfsProp = fac.createPropertyType();
            wfsProp.setName(name);
            wfsProp.setValue(value);
            update.getProperty().add(wfsProp);
        }

        update.setFilter(ff.id(Collections.singleton(ff.featureId(newState.getIdentifier().getID()))));

        return update;
    }

    private DeleteElementType delete(final QName typeName, final String featureId) {
        final WfsFactory fac = WfsFactoryImpl.eINSTANCE;
        DeleteElementType delete = fac.createDeleteElementType();
        delete.setFilter(ff.id(Collections.singleton(ff.featureId(featureId))));
        delete.setTypeName(typeName);
        return delete;
    }

    @SuppressWarnings("unchecked")
    private InsertElementType insert(final QName featureTypeName, final Feature addedFeature) {
        final WfsFactory fac = WfsFactoryImpl.eINSTANCE;
        InsertElementType insert = fac.createInsertElementType();
        insert.getFeature().add(addedFeature);
        return insert;
    }

    /**
     * @return committer
     */
    private List<PersonImpl> contributor(final RevCommit newCommit) {
        PersonImpl contributor = new PersonImpl();
        contributor.setName(newCommit.getCommitter());
        return Collections.singletonList(contributor);
    }

    /**
     * @return commit author
     */
    private List<PersonImpl> author(final RevCommit newCommit) {
        PersonImpl author = new PersonImpl();
        author.setName(newCommit.getAuthor());
        return Collections.singletonList(author);
    }

    private String title(final DiffEntry diff) {
        final String featureId = diff.getPath().get(diff.getPath().size() - 1);
        StringBuilder title = new StringBuilder();
        switch (diff.getType()) {
        case ADD:
            title.append("Insert");
            break;
        case DELETE:
            title.append("Delte");
            break;
        case MODIFY:
            title.append("Update");
            break;
        }
        title.append(" of Feature ").append(featureId);

        return title.toString();
    }

}
