package org.geoserver.data.versioning.decorator;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.repository.Repository;
import org.geogit.storage.ObjectReader;
import org.geogit.storage.StagingDatabase;
import org.geogit.storage.WrappedSerialisingFactory;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.identity.ResourceId;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

public class ResourceIdFeatureCollector<F extends Feature> implements Iterable<F> {

    private final Repository repository;

    private final FeatureType featureType;

    private final Set<ResourceId> resourceIds;

    public ResourceIdFeatureCollector(final Repository repository, final FeatureType featureType,
            final Set<ResourceId> resourceIds) {
        this.repository = repository;
        this.featureType = featureType;
        this.resourceIds = resourceIds;
    }

    @Override
    public Iterator<F> iterator() {

        Iterator<Ref> featureRefs = Iterators.emptyIterator();

        GeoGIT ggit = new GeoGIT(repository);
        VersionQuery query = new VersionQuery(ggit, featureType.getName());
        try {
            for (ResourceId rid : resourceIds) {
                Iterator<Ref> ridIterator;
                ridIterator = query.get(rid);
                featureRefs = Iterators.concat(featureRefs, ridIterator);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Iterator<F> features = Iterators.transform(featureRefs, new RefToFeature<F>(repository,
                featureType));

        return features;
    }

    private static class RefToFeature<F extends Feature> implements Function<Ref, F> {

        private final Repository repo;

        private final FeatureType type;

        private WrappedSerialisingFactory serialisingFactory;

        public RefToFeature(final Repository repo, final FeatureType type) {
            this.repo = repo;
            this.type = type;
            serialisingFactory = WrappedSerialisingFactory.getInstance();
        }

        @Override
        public F apply(final Ref featureRef) {
            String featureId = featureRef.getName();
            ObjectId contentId = featureRef.getObjectId();
            StagingDatabase database = repo.getIndex().getDatabase();
            F feature;
            try {
                @SuppressWarnings("unchecked")
                ObjectReader<F> featureReader = (ObjectReader<F>) serialisingFactory
                        .createFeatureReader(type, featureId);
                feature = database.get(contentId, featureReader);
                checkState(feature != null);
                checkState(featureId.equals(feature.getIdentifier().getID()));
                checkState(feature.getIdentifier().getFeatureVersion() != null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return feature;
        }

    }

}
