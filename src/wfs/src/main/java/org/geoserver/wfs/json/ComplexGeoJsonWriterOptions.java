/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.json;

import java.util.List;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.type.ComplexType;

/**
 * Extension point to define the behaviour regarding non standard mandatory encoding behaviours of
 * the ComplexGeoJsonWriter.
 */
public interface ComplexGeoJsonWriterOptions {

    /**
     * Method to check if the List of FeatureCollection being passed are supported by this specific
     * options.
     *
     * @param features the list of FeatureCollection being encoded by the ComplexGeoJsonWriter
     * @return true if this option can handle the features being encoded false otherwise.
     */
    boolean canHandle(List<FeatureCollection> features);

    /**
     * Method to check if the ComplexGeoJsonWriter should encode the a ComplexAttributeType name
     * using the @dataType key in the final GeoJson output.
     *
     * @return true if @dataType should be included in the output, false otherwise.
     */
    boolean encodeComplexAttributeType();

    /**
     * Method to check if a nested Feature should be encoded as full GeoJson feature, with a
     * properties object, an id and a geometry attribute, or can be encoded as property hence a JSON
     * object.
     *
     * @param complexType the type of the nested Feature.
     * @return true if the ComplexGeoJsonWriter can encode the nested Feature as a property, false
     *     otherwise.
     */
    boolean encodeNestedFeatureAsProperty(ComplexType complexType);
}
