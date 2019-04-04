/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRecord;
import es.unex.sextante.dataObjects.IRecordsetIterator;
import es.unex.sextante.dataObjects.RecordImpl;
import es.unex.sextante.exceptions.IteratorException;
import java.io.IOException;
import org.geotools.data.shapefile.dbf.DbaseFileReader;

/** @author Cesar Martinez Izquierdo */
public class GTDBFIterator implements IRecordsetIterator {
    private DbaseFileReader reader = null;

    public GTDBFIterator(DbaseFileReader reader) {
        this.reader = reader;
    }

    public void close() {
        try {
            this.reader.close();
        } catch (IOException e) {
            Sextante.addErrorToLog(e);
        }
    }

    public boolean hasNext() {
        return reader.hasNext();
    }

    public IRecord next() throws IteratorException {
        if (reader.hasNext()) {
            try {
                return new RecordImpl(reader.readEntry());
            } catch (IOException e) {
                Sextante.addErrorToLog(e);
            }
        }
        throw new IteratorException();
    }
}
