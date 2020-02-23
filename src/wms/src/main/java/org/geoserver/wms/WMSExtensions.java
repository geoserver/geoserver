/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.featureinfo.GetFeatureInfoOutputFormat;
import org.geoserver.wms.map.RenderedImageMapResponse;
import org.springframework.context.ApplicationContext;

/**
 * Utility class uses to process GeoServer WMS extension points.
 *
 * @author Gabriel Roldan
 * @version $Id$
 */
public class WMSExtensions {

    /** Finds out the registered GetMapOutputFormats in the application context. */
    public static List<GetMapOutputFormat> findMapProducers(final ApplicationContext context) {
        return GeoServerExtensions.extensions(GetMapOutputFormat.class, context);
    }

    /**
     * Finds out a {@link GetMapOutputFormat} specialized in generating the requested map format,
     * registered in the spring context.
     *
     * @param outputFormat a request parameter object wich holds the processed request objects, such
     *     as layers, bbox, outpu format, etc.
     * @return A specialization of <code>GetMapDelegate</code> wich can produce the requested output
     *     map format, or {@code null} if none is found
     */
    public static GetMapOutputFormat findMapProducer(
            final String outputFormat, final ApplicationContext applicationContext) {

        final Collection<GetMapOutputFormat> producers;
        producers = WMSExtensions.findMapProducers(applicationContext);

        return findMapProducer(outputFormat, producers);
    }

    /** @return {@link GetMapOutputFormat} for the requested outputFormat, or {@code null} */
    public static GetMapOutputFormat findMapProducer(
            String outputFormat, Collection<GetMapOutputFormat> producers) {

        Set<String> producerFormats;
        for (GetMapOutputFormat producer : producers) {
            producerFormats = producer.getOutputFormatNames();
            Set<String> caseInsensitiveFormats = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveFormats.addAll(producerFormats);
            if (caseInsensitiveFormats.contains(outputFormat)) {
                return producer;
            }
        }
        return null;
    }

    /** @return the configured {@link GetFeatureInfoOutputFormat}s */
    public static List<GetFeatureInfoOutputFormat> findFeatureInfoFormats(
            ApplicationContext applicationContext) {
        return GeoServerExtensions.extensions(GetFeatureInfoOutputFormat.class, applicationContext);
    }

    public static GetLegendGraphicOutputFormat findLegendGraphicFormat(
            final String outputFormat, final ApplicationContext applicationContext) {

        List<GetLegendGraphicOutputFormat> formats = findLegendGraphicFormats(applicationContext);

        for (GetLegendGraphicOutputFormat format : formats) {
            if (format.getContentType().startsWith(outputFormat)) {
                return format;
            }
        }
        return null;
    }

    public static List<GetLegendGraphicOutputFormat> findLegendGraphicFormats(
            final ApplicationContext applicationContext) {
        List<GetLegendGraphicOutputFormat> formats =
                GeoServerExtensions.extensions(
                        GetLegendGraphicOutputFormat.class, applicationContext);
        return formats;
    }

    /** Looks up {@link ExtendedCapabilitiesProvider} extensions. */
    public static List<ExtendedCapabilitiesProvider> findExtendedCapabilitiesProviders(
            final ApplicationContext applicationContext) {
        return GeoServerExtensions.extensions(
                ExtendedCapabilitiesProvider.class, applicationContext);
    }

    /**
     * Looks up all the {@link RenderedImageMapResponse} registered in the Spring application
     * context
     */
    public static Collection<RenderedImageMapResponse> findMapResponses(
            ApplicationContext applicationContext) {
        return GeoServerExtensions.extensions(RenderedImageMapResponse.class, applicationContext);
    }
}
