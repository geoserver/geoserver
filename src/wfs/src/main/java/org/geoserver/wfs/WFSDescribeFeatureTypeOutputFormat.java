/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.io.IOException;
import java.io.OutputStream;

import net.opengis.wfs.FeatureCollectionType;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.geoserver.wfs.response.WFSResponse;


/**
 * Base class for a response to a WFS DescribeFeatureType operation.
 * <p>
 * The result of a DescribeFeatureType operation is an array of
 * {@link FeatureTypeInfo}. Subclasses are responsible for serializing these
 * instances. See  {@link #write(FeatureCollectionType, OutputStream, Operation)}.
 * </p>
 * <p>
 * Subclasses also need declare the mime-type in which the format is encoded.
 * </p>
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public abstract class WFSDescribeFeatureTypeOutputFormat extends WFSResponse {
    /**
     * Constructor which sets the outputFormat.
     *
     * @param outputFormat The well-known name of the format, not <code>null</code>
     */
    public WFSDescribeFeatureTypeOutputFormat(GeoServer gs, String outputFormat) {
        super(gs, FeatureTypeInfo[].class, outputFormat);
    }

    /**
     * Ensures that the operation being executed is a DescribeFeatureType operation.
     * <p>
     * This method may be extended to add additional checks, it should not be
     * overriden.
     * </p>
     */
    public boolean canHandle(Operation operation) {
        if ("DescribeFeatureType".equalsIgnoreCase(operation.getId())) {
            return true;
        }

        return false;
    }

    /**
     * Calls through to {@link #write(FeatureTypeInfo[], OutputStream, Operation)}.
     */
    public final void write(Object value, OutputStream output, Operation operation)
        throws IOException, ServiceException {
        write((FeatureTypeInfo[]) value, output, operation);
    }

    /**
     * Serializes the collection of feature type metadata objects  in the format
     * declared.
     *
     * @param featureTypeInfos The feature type metadata objects to serialize
     * @param output The output stream to serialize to.
     * @param getFeature The DescribeFeatureType operation descriptor.
     */
    protected abstract void write(FeatureTypeInfo[] featureTypeInfos, OutputStream output,
        Operation describeFeatureType) throws IOException;
}
