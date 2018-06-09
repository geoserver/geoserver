/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import es.unex.sextante.dataObjects.IRecord;
import es.unex.sextante.dataObjects.IRecordsetIterator;
import es.unex.sextante.dataObjects.RecordImpl;
import java.util.NoSuchElementException;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class GTRecordsetIterator implements IRecordsetIterator {

    private FeatureIterator<SimpleFeature> m_Iter;

    public GTRecordsetIterator(FeatureCollection<SimpleFeatureType, SimpleFeature> fc) {

        m_Iter = fc.features();
    }

    public boolean hasNext() {

        if (m_Iter != null) {
            return m_Iter.hasNext();
        } else {
            return false;
        }
    }

    public IRecord next() throws NoSuchElementException {

        if (m_Iter != null) {
            if (!m_Iter.hasNext()) {
                throw new NoSuchElementException();
            }
            SimpleFeature gtFeat = m_Iter.next();
            Object values[] = new Object[gtFeat.getAttributeCount()];
            for (int i = 0; i < gtFeat.getAttributeCount(); i++) {
                values[i] = gtFeat.getAttribute(i);
            }
            IRecord rec = new RecordImpl(values);
            return rec;
        } else {
            return null;
        }
    }

    public void close() {

        m_Iter.close();
    }
}
