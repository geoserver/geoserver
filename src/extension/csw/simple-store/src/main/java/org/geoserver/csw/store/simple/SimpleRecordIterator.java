/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.simple;

import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.opengis.cat.csw20.RecordType;
import net.opengis.cat.csw20.SimpleLiteral;
import net.opengis.ows10.BoundingBoxType;
import org.geoserver.csw.records.CSWRecordBuilder;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.csw.CSWConfiguration;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.geotools.xsd.Parser;
import org.opengis.feature.Feature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Builds features scanning xml files in the specified folder, and parsing them as CSW Record
 * objects
 *
 * @author Andrea Aime - GeoSolutions
 */
class SimpleRecordIterator implements Iterator<Feature> {

    static final Logger LOGGER = Logging.getLogger(SimpleRecordIterator.class);

    Iterator<Resource> files;

    RecordType record;

    Resource lastFile;

    Parser parser;

    CSWRecordBuilder builder = new CSWRecordBuilder();

    int offset;

    public SimpleRecordIterator(Resource root, int offset) {
        List<Resource> fileArray = Resources.list(root, new Resources.ExtensionFilter("XML"));
        files = fileArray.iterator();
        parser = new Parser(new CSWConfiguration());
        this.offset = offset;
    }

    @Override
    public boolean hasNext() {
        while ((record == null || offset > 0) && files.hasNext()) {
            Resource file = files.next();
            lastFile = file;
            try (InputStream is = file.in()) {
                record = (RecordType) parser.parse(is);
                if (offset > 0) {
                    offset--;
                    record = null;
                }
            } catch (Exception e) {
                LOGGER.log(
                        Level.INFO,
                        "Failed to parse the contents of " + file.path() + " as a CSW Record",
                        e);
            }
        }

        return record != null;
    }

    @Override
    public Feature next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more records to retrieve");
        }

        Feature next = convertToFeature(record);
        record = null;
        return next;
    }

    private Feature convertToFeature(RecordType r) {
        String id = null;

        // add all the elements
        for (SimpleLiteral sl : r.getDCElement()) {
            Object value = sl.getValue();
            String scheme = sl.getScheme() == null ? null : sl.getScheme().toString();
            String name = sl.getName();
            if (value != null && sl.getName() != null) {
                builder.addElementWithScheme(name, scheme, value.toString());
                if ("identifier".equals(name)) {
                    id = value.toString();
                }
            }
        }

        // move on to the bounding boxes
        for (BoundingBoxType bbox : r.getBoundingBox()) {
            if (bbox != null) {
                CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
                if (bbox.getCrs() != null) {
                    try {
                        crs = CRS.decode(bbox.getCrs());
                    } catch (Exception e) {
                        LOGGER.log(Level.INFO, "Failed to parse original record bbox");
                    }
                }
                ReferencedEnvelope re =
                        new ReferencedEnvelope(
                                (Double) bbox.getLowerCorner().get(0),
                                (Double) bbox.getUpperCorner().get(0),
                                (Double) bbox.getLowerCorner().get(1),
                                (Double) bbox.getUpperCorner().get(1),
                                crs);
                builder.addBoundingBox(re);
            }
        }

        return builder.build(id);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("This iterator is read only");
    }

    public Resource getLastFile() {
        return lastFile;
    }
}
