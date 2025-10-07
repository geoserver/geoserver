/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import java.io.IOException;
import java.io.OutputStream;
import org.geoserver.config.GeoServer;
import org.geoserver.excel.ExcelWriter;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.WFSGetFeatureOutputFormat;
import org.geoserver.wfs.request.FeatureCollectionResponse;

/**
 * Abstract base class for Excel WFS output format
 *
 * @author Sebastian Benthall, OpenGeo, seb@opengeo.org and Shane StClair, Axiom Consulting, shane@axiomalaska.com
 */
public abstract class ExcelOutputFormat extends WFSGetFeatureOutputFormat {

    private final ExcelWriter excelWriter;

    protected String mimeType;
    protected String fileExtension;

    public ExcelOutputFormat(GeoServer gs, String formatName) {
        super(gs, formatName);
        this.excelWriter = new ExcelWriter();
    }

    /** @return The {@link org.geoserver.excel.ExcelWriter.ExcelFormat} that should be used writing the output. */
    protected abstract ExcelWriter.ExcelFormat excelFormat();

    /** @return mime type; */
    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        return mimeType;
    }

    @Override
    protected String getExtension(FeatureCollectionResponse response) {
        return fileExtension;
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        return DISPOSITION_ATTACH;
    }

    /** @see WFSGetFeatureOutputFormat#write(Object, OutputStream, Operation) */
    @Override
    protected void write(FeatureCollectionResponse featureCollection, OutputStream output, Operation getFeature)
            throws IOException, ServiceException {
        excelWriter.write(featureCollection.getFeatures(), output, excelFormat());
    }
}
