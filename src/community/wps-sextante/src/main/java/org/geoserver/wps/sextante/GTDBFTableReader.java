/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.sextante;

import es.unex.sextante.dataObjects.AbstractTable;
import es.unex.sextante.dataObjects.IRecordsetIterator;
import es.unex.sextante.outputs.FileOutputChannel;
import es.unex.sextante.outputs.IOutputChannel;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import org.geotools.data.shapefile.dbf.DbaseFileReader;

public class GTDBFTableReader extends AbstractTable {

    private String m_sName;
    private final String m_sFilename;
    private int m_iRecordCount;
    private final DbaseFileReader m_BaseDataObject;

    public GTDBFTableReader(final String sFilename) throws IOException {

        final FileChannel in = new FileInputStream(sFilename).getChannel();
        final DbaseFileReader r = new DbaseFileReader(in, false, Charset.defaultCharset());
        m_BaseDataObject = r;
        m_sFilename = sFilename;
    }

    private DbaseFileReader getDBFReader() {

        return m_BaseDataObject;
    }

    public void addRecord(final Object[] attributes) {
        // this class is only for reading
    }

    public int getFieldCount() {

        return getDBFReader().getHeader().getNumFields();
    }

    public String getFieldName(final int i) {

        return getDBFReader().getHeader().getFieldName(i);
    }

    public Class getFieldType(final int i) {

        return getDBFReader().getHeader().getFieldClass(i);
    }

    public long getRecordCount() {

        return m_iRecordCount;
    }

    public IRecordsetIterator iterator() {

        return new GTDBFIterator(getDBFReader());
    }

    public void close() {

        try {
            getDBFReader().close();
        } catch (final IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public IOutputChannel getOutputChannel() {

        return new FileOutputChannel(m_sFilename);
    }

    public String getName() {

        return m_sName;
    }

    public void open() {

        m_iRecordCount = 0;
        final DbaseFileReader reader = getDBFReader();
        while (reader.hasNext()) {
            try {
                reader.readRow();
            } catch (final IOException e) {
            }
            m_iRecordCount++;
        }
    }

    public void postProcess() throws Exception {}

    public void setName(final String name) {

        m_sName = name;
    }

    @Override
    public void free() {}

    @Override
    public Object getBaseDataObject() {

        return m_BaseDataObject;
    }
}
