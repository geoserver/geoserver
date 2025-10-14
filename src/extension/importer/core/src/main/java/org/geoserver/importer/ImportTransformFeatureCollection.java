/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.NoSuchElementException;
import org.geoserver.importer.job.ProgressMonitor;
import org.geoserver.importer.transform.VectorTransformChain;
import org.geotools.api.data.DataStore;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.geotools.feature.collection.DecoratingFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;

/**
 * FeatureCollection that does two things required by the importer; a) provide cancel functionality b) Do some
 * FeatureType Transforming
 *
 * <p>This class is simply wraps the FeatureIterator with two iterators wrappers that provide the above functionality.
 */
class ImportTransformFeatureCollection<T extends FeatureType, F extends Feature>
        extends DecoratingFeatureCollection<T, F> {

    ProgressMonitor monitor;

    FeatureDataConverter featureDataConverter;

    FeatureType resultingFT;

    VectorTransformChain vectorTransformChain;

    ImportTask task;

    DataStore dataStoreDestination;

    public ImportTransformFeatureCollection(
            FeatureCollection<T, F> fc,
            FeatureDataConverter featureDataConverter,
            FeatureType resultingFT,
            VectorTransformChain vectorTransformChain,
            ImportTask task,
            DataStore dataStoreDestination) {
        super(fc);
        this.monitor = task.progress();
        this.featureDataConverter = featureDataConverter;
        this.resultingFT = resultingFT;
        this.vectorTransformChain = vectorTransformChain;
        this.task = task;
        this.dataStoreDestination = dataStoreDestination;
    }

    @Override
    public FeatureIterator<F> features() {
        return new TransformingFeatureIterator<>(
                new CancelableFeatureIterator<>(super.features(), monitor),
                resultingFT,
                featureDataConverter,
                vectorTransformChain,
                task,
                dataStoreDestination);
    }

    /**
     * Simple FeatureIterator that does some transforming of the features.
     *
     * <p>The emulates the behavior of the Importer's low-level feature transformation.
     */
    private static class TransformingFeatureIterator<F extends Feature> extends DecoratingFeatureIterator<F> {

        SimpleFeatureBuilder featureBuilder;

        FeatureDataConverter featureDataConverter;

        VectorTransformChain vectorTransformChain;

        ImportTask task;

        DataStore dataStore;

        int cnt = 0;

        public TransformingFeatureIterator(
                FeatureIterator<F> fi,
                FeatureType resultingFT,
                FeatureDataConverter featureDataConverter,
                VectorTransformChain vectorTransformChain,
                ImportTask task,
                DataStore dataStore) {
            super(fi);
            this.featureBuilder = new SimpleFeatureBuilder((SimpleFeatureType) resultingFT);
            this.featureDataConverter = featureDataConverter;
            this.vectorTransformChain = vectorTransformChain;
            this.task = task;
            this.dataStore = dataStore;
        }

        @Override
        public F next() throws NoSuchElementException {
            // the xform could produce null features - we eat them
            while (super.hasNext()) {
                F result = attemptNext();
                if (result != null) {
                    return result;
                }
            }
            throw new NoSuchElementException();
        }

        /* for details, see the low-level api version in the Importer */
        private F attemptNext() {
            SimpleFeature input = (SimpleFeature) super.next();
            SimpleFeature result = featureBuilder.buildFeature(null);
            featureDataConverter.convert(input, result);

            // @hack #45678 - mask empty geometry or postgis will complain
            Geometry geom = (Geometry) result.getDefaultGeometry();
            if (geom != null && geom.isEmpty()) {
                result.setDefaultGeometry(null);
            }

            try {
                result = vectorTransformChain.inline(task, dataStore, input, result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            task.setNumberProcessed(++cnt);

            // The above only works with simple features, even if the rest pretends to be generic
            @SuppressWarnings("unchecked")
            F f = (F) result;
            return f;
        }
    }

    /**
     * Simple FeatureIterator that will handle canceling. If the monitor cancels, the iterator will say there are no
     * more elementss (hasNext() will be false)
     */
    private static class CancelableFeatureIterator<F extends Feature> extends DecoratingFeatureIterator<F> {
        ProgressMonitor monitor;

        public CancelableFeatureIterator(FeatureIterator<F> fi, ProgressMonitor monitor) {
            super(fi);
            this.monitor = monitor;
        }

        /** if cancelled, then report no more features */
        @Override
        public boolean hasNext() {
            if (monitor.isCanceled()) {
                return false;
            }
            return super.hasNext();
        }
    }
}
