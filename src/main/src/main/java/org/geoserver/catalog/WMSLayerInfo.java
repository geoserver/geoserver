/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.geotools.api.style.Style;
import org.geotools.api.util.ProgressListener;
import org.geotools.ows.wms.Layer;

public interface WMSLayerInfo extends ResourceInfo {

    @Override
    public WMSStoreInfo getStore();

    /** Returns the raw WMS layer associated to this resource */
    public Layer getWMSLayer(ProgressListener listener) throws IOException;

    /** Return the DataURLs associated with this */
    public List<String> remoteStyles();

    public String getForcedRemoteStyle();

    public void setForcedRemoteStyle(String forcedRemoteStyle);

    public List<String> availableFormats();

    public Optional<Style> findRemoteStyleByName(final String name);

    public boolean isSelectedRemoteStyles(String name);

    public Set<StyleInfo> getRemoteStyleInfos();

    public List<String> getSelectedRemoteFormats();

    public void setSelectedRemoteFormats(List<String> selectedRemoteFormats);

    public List<String> getSelectedRemoteStyles();

    public void setSelectedRemoteStyles(List<String> selectedRemoteStyles);

    public boolean isFormatValid(String format);

    void reset();

    public String getPreferredFormat();

    public void setPreferredFormat(String prefferedFormat);

    public Set<StyleInfo> getStyles();

    public StyleInfo getDefaultStyle();

    public Double getMinScale();

    public void setMinScale(Double minScale);

    public Double getMaxScale();

    public void setMaxScale(Double maxScale);

    public boolean isMetadataBBoxRespected();

    public void setMetadataBBoxRespected(boolean metadataBBoxRespected);

    public List<StyleInfo> getAllAvailableRemoteStyles();

    public Map<String, String> getVendorParameters();

    public void setVendorParameters(Map<String, String> vendorParameters);
}
