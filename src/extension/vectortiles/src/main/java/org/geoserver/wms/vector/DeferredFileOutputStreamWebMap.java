/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.commons.io.output.DeferredFileOutputStream;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.RawMap;

/** WebMap that uses a DeferredFileOutputStream for its content rather than a byte array. */
public class DeferredFileOutputStreamWebMap extends RawMap {

    private DeferredFileOutputStream mapContents;

    /**
     * @param mapContent Unencoded map content
     * @param mapContents Stream to which the encoded map has been written. This will be closed.
     * @param mimeType Format of the map
     */
    public DeferredFileOutputStreamWebMap(
            WMSMapContent mapContent, DeferredFileOutputStream mapContents, String mimeType)
            throws IOException {

        super(mapContent, (byte[]) null, mimeType);
        // make sure the stream is closed to be able of retrieving its contents
        mapContents.close();
        this.mapContents = mapContents;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        mapContents.writeTo(out);
    }

    @Override
    public void disposeInternal() {
        File file = mapContents.getFile();
        if (file != null) {
            file.delete();
        }
    }
}
