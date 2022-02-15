/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.api.feature;

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gsr.api.AbstractGSRController;
import org.geoserver.gsr.model.AbstractGSRModel.Link;
import org.geoserver.gsr.model.feature.FeatureWrapper;
import org.geoserver.gsr.model.geometry.SpatialReference;
import org.geoserver.gsr.model.map.LayerOrTable;
import org.geoserver.gsr.translate.feature.FeatureEncoder;
import org.geoserver.gsr.translate.geometry.SpatialReferences;
import org.geoserver.gsr.translate.map.LayerDAO;
import org.geoserver.ogcapi.HTMLResponseBody;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller for the Feature Service feature list endpoint */
@RestController
@RequestMapping(
        path = "/gsr/services/{workspaceName}/FeatureServer",
        produces = MediaType.APPLICATION_JSON_VALUE)
public class FeatureController extends AbstractGSRController {

    @Autowired
    public FeatureController(@Qualifier("geoServer") GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(path = "/{layerId}/{featureId}", name = "MapServerGetLegend")
    @HTMLResponseBody(templateName = "featureitem.ftl", fileName = "featureitem.html")
    public FeatureWrapper getFeature(
            @PathVariable String workspaceName,
            @PathVariable Integer layerId,
            @PathVariable String featureId)
            throws IOException, FactoryException {
        LayerOrTable l = LayerDAO.find(catalog, workspaceName, layerId);

        if (null == l) {
            throw new NoSuchElementException(
                    "No table or layer in workspace \"" + workspaceName + "\" for id " + layerId);
        }

        FeatureTypeInfo featureType = (FeatureTypeInfo) l.layer.getResource();
        if (null == featureType) {
            throw new NoSuchElementException(
                    "No table or layer in workspace \"" + workspaceName + "\" for id " + layerId);
        }

        Filter idFilter =
                FILTERS.id(
                        FILTERS.featureId(
                                featureType.getFeatureType().getName().getLocalPart()
                                        + "."
                                        + featureId));

        FeatureSource<?, ?> source = featureType.getFeatureSource(null, null);
        FeatureCollection<?, ?> featureColl = source.getFeatures(idFilter);
        org.opengis.feature.Feature[] featureArr =
                featureColl.toArray(new org.opengis.feature.Feature[0]);
        if (featureArr.length == 0) {
            throw new NoSuchElementException(
                    "No feature in layer or table " + layerId + " with id " + featureId);
        }
        SpatialReference spatialReference =
                SpatialReferences.fromCRS(
                        featureArr[0]
                                .getDefaultGeometryProperty()
                                .getDescriptor()
                                .getCoordinateReferenceSystem());
        FeatureWrapper feature =
                new FeatureWrapper(FeatureEncoder.feature(featureArr[0], true, spatialReference));
        feature.getPath()
                .addAll(
                        Arrays.asList(
                                new Link(workspaceName, workspaceName),
                                new Link(workspaceName + "/" + "FeatureServer", "FeatureServer"),
                                new Link(
                                        workspaceName + "/" + "FeatureServer/" + layerId,
                                        l.getName()),
                                new Link(
                                        workspaceName
                                                + "/"
                                                + "FeatureServer/"
                                                + layerId
                                                + "/"
                                                + featureId,
                                        featureId)));
        feature.getInterfaces()
                .add(
                        new Link(
                                workspaceName
                                        + "/"
                                        + "FeatureServer/"
                                        + layerId
                                        + "/"
                                        + featureId
                                        + "?f=json&pretty=true",
                                "REST"));
        return feature;
    }
}
