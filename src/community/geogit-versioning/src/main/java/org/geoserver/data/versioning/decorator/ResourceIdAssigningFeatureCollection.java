package org.geoserver.data.versioning.decorator;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevTree;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.identity.ResourceId;

import com.google.common.collect.AbstractIterator;

/**
 * FeatureCollectionDecorator that assigns as {@link ResourceId} as each Feature
 * {@link Feature#getIdentifier() identifier} from the {@link ObjectId} of the current state of the
 * Feature.
 * 
 * @author groldan
 * 
 */
class ResourceIdAssigningFeatureCollection<T extends FeatureType, F extends Feature> extends
        DecoratingFeatureCollection<T, F> implements FeatureCollection<T, F> {

    protected final FeatureSourceDecorator<T, F> store;

    protected final RevTree currentTypeTree;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected ResourceIdAssigningFeatureCollection(final FeatureCollection delegate,
            final FeatureSourceDecorator<T, F> store, final RevTree currentTypeTree) {

        super(delegate);

        this.store = store;
        this.currentTypeTree = currentTypeTree;
    }

    private class ResourceIdAssigningIterator extends AbstractIterator<F> {

        private final Iterator<F> iterator;

        public ResourceIdAssigningIterator(final Iterator<F> iterator) {
            this.iterator = iterator;
        }

        @Override
        protected F computeNext() {
            if (!iterator.hasNext()) {
                return endOfData();
            }
            F next = iterator.next();
            String featureId = next.getIdentifier().getID();
            Ref ref = currentTypeTree.get(featureId);
            String versionId = ref == null ? null : ref.getObjectId().toString();
            return VersionedFeatureWrapper.wrap(next, versionId);
        }

    }

    @Override
    public Iterator<F> iterator() {
        @SuppressWarnings("deprecation")
        Iterator<F> iterator = delegate.iterator();
        return new ResourceIdAssigningIterator(iterator);
    }

    protected static class ResourceIdAssigningFeatureIterator<G extends Feature> implements
            FeatureIterator<G> {

        protected FeatureIterator<G> features;

        private final FeatureSourceDecorator source;

        private final RevTree typeTree;

        public ResourceIdAssigningFeatureIterator(FeatureIterator<G> features,
                FeatureSourceDecorator source, RevTree currentTypeTree) {
            this.features = features;
            this.source = source;
            this.typeTree = currentTypeTree;
        }

        @Override
        public boolean hasNext() {
            return features.hasNext();
        }

        @Override
        public G next() throws NoSuchElementException {
            G next = features.next();
            String featureId = next.getIdentifier().getID();
            Ref ref = typeTree.get(featureId);
            String versionId = ref == null ? null : ref.getObjectId().toString();
            return VersionedFeatureWrapper.wrap(next, versionId);
        }

        @Override
        public void close() {
            features.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public FeatureIterator<F> features() {
        return new ResourceIdAssigningFeatureIterator(delegate.features(), store, currentTypeTree);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void close(Iterator<F> close) {
        delegate.close(((ResourceIdAssigningIterator) close).iterator);
    }
}
