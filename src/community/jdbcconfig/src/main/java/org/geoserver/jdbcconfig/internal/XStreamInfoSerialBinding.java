/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;

public class XStreamInfoSerialBinding {

    private final XStreamPersister xstreamPersister;

    public XStreamInfoSerialBinding(final XStreamPersisterFactory xspf) {
        this.xstreamPersister = xspf.createXMLPersister();
        this.xstreamPersister.setLoggingLevel(Level.WARNING);
        // new JDBCConfigXStreamPersisterInitializer().init(this.xstreamPersister);
    }

    public <T extends Info> T entryToObject(InputStream in, Class<T> target) {
        // try {
        // in = new LZFInputStream(in);
        // } catch (Exception e) {
        // throw Throwables.propagate(e);
        // }
        T info;

        try {
            info = xstreamPersister.load(in, target);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return info;
    }

    public byte[] objectToEntry(final Info info) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        objectToEntry(info, out);
        return out.toByteArray();
    }

    public void objectToEntry(final Info info, OutputStream out) {

        // out = new LZFOutputStream(out);

        try {
            xstreamPersister.save(info, out);
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setCatalog(Catalog catalog) {
        xstreamPersister.setCatalog(catalog);
    }
}
