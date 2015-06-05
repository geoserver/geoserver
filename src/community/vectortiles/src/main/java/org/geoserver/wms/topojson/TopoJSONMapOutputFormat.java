/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.topojson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Set;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.AbstractMapOutputFormat;
import org.geoserver.wms.map.RawMap;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableSet;

public class TopoJSONMapOutputFormat extends AbstractMapOutputFormat {

    public static final String MIME_TYPE = "application/json";

    public static final Set<String> OUTPUT_FORMATS = ImmutableSet.of(MIME_TYPE, "topojson");

    public TopoJSONMapOutputFormat() {
        super(MIME_TYPE, OUTPUT_FORMATS);
    }

    /**
     * @return {@code null}
     */
    @Override
    public MapProducerCapabilities getCapabilities(final String format) {
        return null;
    }

    @Override
    public RawMap produceMap(final WMSMapContent mapContent) throws ServiceException, IOException {
        // do something.... and then:
        Topology topology = new Topology();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TopoJSONEncoder encoder = new TopoJSONEncoder();
        
        Writer writer = new OutputStreamWriter(out, Charsets.UTF_8);
        encoder.encode(topology, writer);
        writer.flush();
        byte[] mapContents = out.toByteArray();
        RawMap map = new RawMap(mapContent, mapContents, MIME_TYPE);
        map.setResponseHeader("Content-Length", String.valueOf(mapContents.length));
        return map;
    }

}
