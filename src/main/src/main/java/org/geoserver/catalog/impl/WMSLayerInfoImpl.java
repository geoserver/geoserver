/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import com.google.common.base.Objects;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyleFactory;
import org.geotools.api.util.ProgressListener;
import org.geotools.brewer.styling.builder.StyleBuilder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.StyleImpl;

@SuppressWarnings("serial")
public class WMSLayerInfoImpl extends ResourceInfoImpl implements WMSLayerInfo {

    public static final StyleFactory STYLE_FACTORY = CommonFactoryFinder.getStyleFactory();
    // will style info with empty name
    // intended for legacy functionaloty
    // of using default remote style
    public static StyleInfo DEFAULT_ON_REMOTE;

    static {
        DEFAULT_ON_REMOTE = new StyleInfoImpl();
        DEFAULT_ON_REMOTE.setName("");
        DEFAULT_ON_REMOTE.getMetadata().put("isRemote", true);
    }

    public static String DEFAULT_FORMAT = "image/png";

    protected String forcedRemoteStyle = "";
    protected String preferredFormat = DEFAULT_FORMAT;

    private List<String> selectedRemoteFormats = new ArrayList<>();

    private List<String> selectedRemoteStyles = new ArrayList<>();

    private Double minScale = null;

    private Double maxScale = null;

    private boolean metadataBBoxRespected = false;

    private List<StyleInfo> allAvailableRemoteStyles = new ArrayList<>();

    protected WMSLayerInfoImpl() {}

    public WMSLayerInfoImpl(Catalog catalog) {
        super(catalog);
    }

    @Override
    public Layer getWMSLayer(ProgressListener listener) throws IOException {
        return catalog.getResourcePool().getWMSLayer(this);
    }

    @Override
    public void accept(CatalogVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public WMSStoreInfo getStore() {
        return (WMSStoreInfo) super.getStore();
    }

    @Override
    public void reset() {
        selectedRemoteStyles.clear();
        selectedRemoteFormats.clear();
        // set empty to take whatever is on remote server
        forcedRemoteStyle = DEFAULT_ON_REMOTE.getName();
        // select all formats for us
        selectedRemoteFormats.addAll(availableFormats());
        getAllAvailableRemoteStyles().clear();
        getAllAvailableRemoteStyles().addAll(getRemoteStyleInfos());
        // select all formats for use
        selectedRemoteStyles.addAll(remoteStyles());
    }

    @Override
    public List<String> remoteStyles() {

        try {
            return allAvailableRemoteStyles.stream().map(s -> s.getName()).collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to fetch styles for cascaded wms layer " + getNativeName());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getForcedRemoteStyle() {
        return forcedRemoteStyle;
    }

    @Override
    public void setForcedRemoteStyle(String forcedRemoteStyle) {
        this.forcedRemoteStyle = (forcedRemoteStyle == null) ? DEFAULT_ON_REMOTE.getName() : forcedRemoteStyle;
    }

    @Override
    public List<String> availableFormats() {
        try {
            return getStore().getWebMapServer(null).getCapabilities().getRequest().getGetMap().getFormats().stream()
                    .filter(WMSLayerInfoImpl::isImage)
                    . // only image formats
                    collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to fetch available formats for cascaded layer " + getNativeName());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    public String getPreferredFormat() {
        return this.preferredFormat;
    }

    @Override
    public void setPreferredFormat(String preferredFormat) {
        this.preferredFormat = (preferredFormat == null) ? DEFAULT_FORMAT : preferredFormat;
    }

    @Override
    public boolean isFormatValid(String format) {
        if (preferredFormat.equalsIgnoreCase(format)) return true;
        else return selectedRemoteFormats.contains(format);
    }

    public static boolean isImage(String format) {
        return format.startsWith("image") && !format.contains("xml") && !format.contains("svg");
    }

    @Override
    public Set<StyleInfo> getStyles() {
        // no remote styles were read from this server
        if (allAvailableRemoteStyles == null) return null;
        else
            return allAvailableRemoteStyles.stream()
                    .filter(s -> !forcedRemoteStyle.equalsIgnoreCase(s.getName()))
                    .filter(s -> selectedRemoteStyles.contains(s.getName()))
                    .collect(Collectors.toSet());
    }

    @Override
    public StyleInfo getDefaultStyle() {
        if (forcedRemoteStyle != null)
            if (!forcedRemoteStyle.isEmpty()) {
                Optional<StyleInfo> defaultRemoteStyle = getAllAvailableRemoteStyles().stream()
                        .filter(s -> s.getName().equalsIgnoreCase(forcedRemoteStyle))
                        .findFirst();
                // will return null if forcedRemoteStyle is not empty string
                // and was not found in selected remote styles
                if (defaultRemoteStyle.isPresent()) return defaultRemoteStyle.get();
                else return DEFAULT_ON_REMOTE;
            } else {
                return DEFAULT_ON_REMOTE;
            }

        return null;
    }

    @Override
    public Set<StyleInfo> getRemoteStyleInfos() {

        try {
            return getWMSLayer(null).getStyles().stream()
                    .map(WMSLayerInfoImpl::getStyleInfo)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to fetch available styles for cascaded layer " + getNativeName());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        // on error default to super
        return Collections.emptySet();
    }

    @Override
    public Optional<Style> findRemoteStyleByName(final String name) {

        // temp wrapper for the remote style name
        final Style style = new StyleBuilder().name(name).buildStyle();
        try {
            return getWMSLayer(null).getStyles().stream()
                    .filter(s -> s.getName().equalsIgnoreCase(name))
                    .map(s -> style)
                    .findFirst();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to fetch available styles for cascaded layer " + getNativeName());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static StyleInfo getStyleInfo(StyleImpl gtWmsStyle) {

        StyleInfoImpl styleInfo = new StyleInfoImpl();
        // do not set an id, this is not a persiste object, it could otherwise confuse custom
        // catalog facades (e..g, JDBCConfig)
        styleInfo.setName(gtWmsStyle.getName());
        // a hint
        styleInfo.getMetadata().put("isRemote", true);

        LegendInfo remoteLegendInfo = new LegendInfoImpl();
        remoteLegendInfo.setOnlineResource((String) gtWmsStyle.getLegendURLs().get(0));
        remoteLegendInfo.setHeight(20);
        remoteLegendInfo.setWidth(20);
        remoteLegendInfo.setFormat("image/png");
        styleInfo.setLegend(remoteLegendInfo);

        return styleInfo;
    }

    public static Style getStyleInfo(StyleInfo styleInfo) {
        return new StyleBuilder().name(styleInfo.getName()).buildStyle();
    }

    @Override
    public boolean isSelectedRemoteStyles(String name) {
        if (name == null) return false;
        else if (name.isEmpty()) return true;
        else if (forcedRemoteStyle.equalsIgnoreCase(name)) return true;
        else return selectedRemoteStyles.contains(name);
    }

    @Override
    public List<String> getSelectedRemoteFormats() {
        return selectedRemoteFormats;
    }

    @Override
    public void setSelectedRemoteFormats(List<String> selectedRemoteFormats) {
        this.selectedRemoteFormats = selectedRemoteFormats;
    }

    @Override
    public List<String> getSelectedRemoteStyles() {
        return selectedRemoteStyles;
    }

    @Override
    public void setSelectedRemoteStyles(List<String> selectedRemoteStyles) {
        this.selectedRemoteStyles = selectedRemoteStyles;
    }

    @Override
    public Double getMinScale() {
        return minScale;
    }

    @Override
    public void setMinScale(Double minScale) {
        this.minScale = minScale;
    }

    @Override
    public Double getMaxScale() {
        return maxScale;
    }

    @Override
    public void setMaxScale(Double maxScale) {
        this.maxScale = maxScale;
    }

    @Override
    public List<StyleInfo> getAllAvailableRemoteStyles() {
        if (allAvailableRemoteStyles == null) allAvailableRemoteStyles = new ArrayList<>();
        return allAvailableRemoteStyles;
    }

    private Map<String, String> vendorParameters = new HashMap<>();

    // This is only called by OWS with a null which should be fine
    @Override
    public Map<String, String> getVendorParameters() {
        return vendorParameters;
    }

    @Override
    public void setVendorParameters(Map<String, String> vendorParameters) {
        if (vendorParameters != null) {
            this.vendorParameters = vendorParameters;
        } else this.vendorParameters = new HashMap<>();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((forcedRemoteStyle == null) ? 0 : forcedRemoteStyle.hashCode());
        result = prime * result + ((preferredFormat == null) ? 0 : preferredFormat.hashCode());
        result = prime * result + ((selectedRemoteFormats == null) ? 0 : selectedRemoteFormats.hashCode());
        result = prime * result + ((selectedRemoteStyles == null) ? 0 : selectedRemoteStyles.hashCode());
        result = prime * result + ((allAvailableRemoteStyles == null) ? 0 : allAvailableRemoteStyles.hashCode());
        result = prime * result + ((minScale == null) ? 0 : minScale.hashCode());
        result = prime * result + ((maxScale == null) ? 0 : maxScale.hashCode());
        result = prime * result + Boolean.hashCode(metadataBBoxRespected);
        result = prime * result + ((vendorParameters == null) ? 0 : vendorParameters.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WMSLayerInfo)) return false;
        if (!super.equals(obj)) return false;

        WMSLayerInfo other = (WMSLayerInfo) obj;
        if (!Objects.equal(forcedRemoteStyle, other.getForcedRemoteStyle())) return false;
        if (!Objects.equal(preferredFormat, other.getPreferredFormat())) return false;
        if (!Objects.equal(selectedRemoteFormats, other.getSelectedRemoteFormats())) return false;
        if (!Objects.equal(selectedRemoteStyles, other.getSelectedRemoteStyles())) return false;
        if (!Objects.equal(allAvailableRemoteStyles, other.getAllAvailableRemoteStyles())) return false;
        if (!Objects.equal(minScale, other.getMinScale())) return false;
        if (!Objects.equal(maxScale, other.getMaxScale())) return false;
        if (!(other.isMetadataBBoxRespected() == this.metadataBBoxRespected)) return false;
        if (!Objects.equal(vendorParameters, other.getVendorParameters())) return false;

        return true;
    }

    @Override
    public boolean isMetadataBBoxRespected() {
        return metadataBBoxRespected;
    }

    @Override
    public void setMetadataBBoxRespected(boolean metadataBBoxRespected) {
        this.metadataBBoxRespected = metadataBBoxRespected;
    }
}
