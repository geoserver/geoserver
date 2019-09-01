/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.geotools.styling.Style;

/** @author ImranR */
public interface CascadedLayerInfo extends LayerInfo {
    public List<String> getRemoteStyles();

    public String getForcedRemoteStyle();

    public void setForcedRemoteStyle(String forcedRemoteStyle);

    public List<String> getAvailableFormats();

    public Optional<Style> findRemoteStyleByName(final String name);

    public boolean isSelectedRemoteStyles(String name);

    public Set<StyleInfo> getRemoteStyleInfos();

    public List<String> getSelectedRemoteFormats();

    public void setSelectedRemoteFormats(List<String> selectedRemoteFormats);

    public List<String> getSelectedRemoteStyles();

    public void setSelectedRemoteStyles(List<String> selectedRemoteStyles);

    public boolean isSelectedRemoteFormat(String format);

    void reset();

    public String getPrefferedFormat();

    public void setPrefferedFormat(String prefferedFormat);
}
