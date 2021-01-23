package org.geoserver.ogcapi.dggs;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.geotools.data.CloseableIterator;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.dggs.GroupedMatrixAggregate;
import org.geotools.dggs.GroupedMatrixAggregate.GroupByResult;
import org.geotools.feature.collection.FilteringSimpleFeatureCollection;
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.FeatureVisitor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.util.ProgressListener;

/**
 * A feature colleciton building on top of a {@link GroupedMatrixAggregate.IterableResult} and a
 * {@link SimpleFeatureBuilder}. Assumptions:
 *
 * <ul>
 *   <li>There is a fixed geometry, the aggregation one, that is provided statically
 *   <li>The aggregation keys are the first attributes
 *   <li>The computed values are following them
 * </ul>
 */
class GroupMatrixFeatureCollection implements SimpleFeatureCollection {

    private final SimpleFeatureType schema;
    private final GroupedMatrixAggregate.IterableResult result;
    private final Function<GroupByResult, SimpleFeature> featureMapper;

    public GroupMatrixFeatureCollection(
            SimpleFeatureType schema,
            GroupedMatrixAggregate.IterableResult result,
            Function<GroupByResult, SimpleFeature> featureMapper) {
        this.schema = schema;
        this.result = result;
        this.featureMapper = featureMapper;
    }

    @Override
    public SimpleFeatureIterator features() {
        return new GroupMatrixFeatureIterator(result.getIterator());
    }

    @Override
    public SimpleFeatureType getSchema() {
        return schema;
    }

    @Override
    public String getID() {
        return null;
    }

    @Override
    public void accepts(FeatureVisitor visitor, ProgressListener progress) throws IOException {
        DataUtilities.visit(this, visitor, progress);
    }

    @Override
    public SimpleFeatureCollection subCollection(Filter filter) {
        return new FilteringSimpleFeatureCollection(this, filter);
    }

    @Override
    public SimpleFeatureCollection sort(SortBy order) {
        return new SortedSimpleFeatureCollection(this, new SortBy[] {order});
    }

    @Override
    public ReferencedEnvelope getBounds() {
        return DataUtilities.bounds(this);
    }

    @Override
    public boolean contains(Object o) {
        try (CloseableIterator<GroupByResult> it = result.getIterator()) {
            while (it.hasNext()) {
                if (Objects.equals(o, it.next())) return true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    @Override
    public boolean containsAll(Collection<?> o) {
        Set<Object> test = new HashSet<>(o);
        try (CloseableIterator<GroupByResult> it = result.getIterator()) {
            while (it.hasNext() && !test.isEmpty()) {
                test.remove(it.next());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return test.isEmpty();
    }

    @Override
    public boolean isEmpty() {
        try (CloseableIterator<GroupByResult> it = result.getIterator()) {
            return features().hasNext();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int size() {
        return DataUtilities.count(features());
    }

    @Override
    public Object[] toArray() {
        return DataUtilities.list(this).toArray();
    }

    @Override
    public <O> O[] toArray(O[] a) {
        return DataUtilities.list(this).toArray(a);
    }

    private class GroupMatrixFeatureIterator implements SimpleFeatureIterator {

        CloseableIterator<GroupByResult> iterator;

        public GroupMatrixFeatureIterator(CloseableIterator<GroupByResult> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public SimpleFeature next() throws NoSuchElementException {
            GroupByResult result = iterator.next();
            return featureMapper.apply(result);
        }

        @Override
        public void close() {
            try {
                iterator.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
