/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.fs.service;

import org.opengeo.gsr.core.geometry.SpatialReference;
import org.opengeo.gsr.core.geometry.Envelope;
import org.opengeo.gsr.ms.resource.LayerOrTable;
import org.opengeo.gsr.service.AbstractService;

import java.util.List;

/**
 * @author David Winslow, Boundless
 */
public class FeatureService implements AbstractService {
    private double currentVersion;
    private String serviceDescription;
    private boolean hasVersionedData;
    private boolean supportsDisconnectedEditing;
    private boolean hasStaticData;
    private int maxRecordCount;
    private String supportedQueryFormats;
    private String capabilities;
    private String description;
    private String copyrightText;
    private SpatialReference spatialReference;
    private Envelope initialExtent;
    private Envelope fullExtent;
    private boolean allowGeometryUpdates;
    private String units;
    private boolean syncEnabled;
    private SyncCapabilities syncCapabilities;
    private EditorTrackingInfo editorTrackingInfo;
    private DocumentInfo documentInfo;
    private List<LayerOrTable> layers;
    private List<LayerOrTable> tables;
    private boolean enableZDefaults;
    private double zDefault;

    public FeatureService() {
        currentVersion = 10.21;
        serviceDescription = "";
        hasVersionedData = false;
        supportsDisconnectedEditing = false;
        hasStaticData = false;
        maxRecordCount = 0;
        supportedQueryFormats = "JSON";
        capabilities = "";
        description = "";
        copyrightText = "";
        spatialReference = null; // TODO
        initialExtent = null;
        fullExtent = null;
        allowGeometryUpdates = false;
        units = "";
        syncEnabled = false;
        syncCapabilities = null;
        editorTrackingInfo = null;
        documentInfo = null;
        layers = null;
        tables = null;
        enableZDefaults = false;
        zDefault = 0;
    }
}
