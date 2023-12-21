/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geotools.data.graticule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureReader;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.graticule.gridsupport.LineFeatureBuilder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.grid.Lines;
import org.geotools.grid.ortholine.LineOrientation;
import org.geotools.grid.ortholine.OrthoLineDef;

public class GraticuleFeatureReader implements SimpleFeatureReader {
    private final List<Double> steps;
    private SimpleFeatureSource currentGrid;
    private SimpleFeatureIterator delegate;
    SimpleFeatureType schema;
    GraticuleDataStore parent;
    Query query;
    int level = 0;

    public GraticuleFeatureReader(GraticuleDataStore graticuleDataStore, Query query)
            throws IOException {
        this.parent = graticuleDataStore;
        this.query = query;
        this.steps = parent.getSteps();
        schema = parent.getSchema(query.getTypeName());
        currentGrid = buildGrid(parent.bounds);
        delegate = currentGrid.getFeatures(query).features();
    }

    /**
     * Return the FeatureType this reader has been configured to create.
     *
     * @return the FeatureType of the Features this FeatureReader will create.
     */
    @Override
    public SimpleFeatureType getFeatureType() {
        return schema;
    }

    public SimpleFeatureIterator getDelegate() {
        return delegate;
    }

    /**
     * Reads the next Feature in the FeatureReader.
     *
     * @return The next feature in the reader.
     * @throws IOException If an error occurs reading the Feature.
     * @throws IllegalAttributeException If the attributes read do not comply with the FeatureType.
     * @throws NoSuchElementException If there are no more Features in the Reader.
     */
    @Override
    public SimpleFeature next() throws IllegalArgumentException, NoSuchElementException {
        SimpleFeature next = delegate.next();
        return next;
    }

    /**
     * Query whether this FeatureReader has another Feature.
     *
     * @return True if there are more Features to be read. In other words, true if calls to next
     *     would return a feature rather than throwing an exception.
     * @throws IOException If an error occurs determining if there are more Features.
     */
    @Override
    public boolean hasNext() throws IOException {
        return delegate.hasNext();
    }

    /**
     * Release the underlying resources associated with this stream.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        delegate.close();
    }

    private SimpleFeatureSource buildGrid(ReferencedEnvelope box) {
        List<OrthoLineDef> lineDefs = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            double step = steps.get(i);

            // vertical (longitude) lines
            lineDefs.add(new OrthoLineDef(LineOrientation.VERTICAL, i, step));
            // horizontal (latitude) lines
            lineDefs.add(new OrthoLineDef(LineOrientation.HORIZONTAL, i, step));
        }

        SimpleFeatureSource grid =
                Lines.createOrthoLines(box, lineDefs, steps.get(0), new LineFeatureBuilder(schema));
        return grid;
    }
}
