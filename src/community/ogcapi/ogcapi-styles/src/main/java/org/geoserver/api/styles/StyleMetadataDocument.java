/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.geoserver.api.AbstractDocument;
import org.geoserver.api.NCNameResourceCodec;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServer;
import org.geotools.styling.Description;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.util.Version;

/** Style metadadata information */
@JsonInclude(NON_NULL)
public class StyleMetadataDocument extends AbstractDocument implements Serializable {

    String id;
    String title;
    boolean titleSet;
    String description;
    boolean descriptionSet;
    List<String> keywords;
    boolean keywordsSet;
    String pointOfContact;
    boolean pointOfContactSet;
    String accessConstraints;
    boolean accessConstraintsSet;
    StyleDates dates;
    boolean datesSet;
    String scope = "style";
    List<Stylesheet> stylesheets = new ArrayList<>();
    List<StyleLayer> layers = new ArrayList<>();

    public StyleMetadataDocument() {
        // empty constructor for Jackson usage
    }

    public StyleMetadataDocument(StyleInfo si, GeoServer gs) throws IOException {
        this.id = NCNameResourceCodec.encode(si);
        StyledLayerDescriptor sld = si.getSLD();
        Optional<StyleMetadataInfo> styleMetadata =
                Optional.ofNullable(
                        si.getMetadata()
                                .get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class));
        this.title = styleMetadata.map(StyleMetadataInfo::getTitle).orElseGet(() -> getTitle(sld));
        this.description =
                styleMetadata
                        .map(StyleMetadataInfo::getAbstract)
                        .orElseGet(() -> getDescription(sld));
        this.keywords = styleMetadata.map(StyleMetadataInfo::getKeywords).orElse(null);
        this.pointOfContact =
                styleMetadata
                        .map(StyleMetadataInfo::getPointOfContact)
                        .orElseGet(() -> gs.getSettings().getContact().getContactPerson());
        this.accessConstraints =
                styleMetadata.map(StyleMetadataInfo::getAccessConstraints).orElse("unclassified");

        // TODO: dates

        // stylesheet links
        for (StyleHandler handler : Styles.handlers()) {
            for (Version version : handler.getVersions()) {
                Stylesheet ss = new Stylesheet(si, handler, version);
                if (ss.isNative() || handler.supportsEncoding(version)) {
                    stylesheets.add(ss);
                }
            }
        }

        // layers links
        StyledLayer[] styledLayers = sld.getStyledLayers();

        if (styledLayers.length == 1) {
            // common GeoServer case, there is a single layer referenced and we use only the style
            // portion, not the layer one, we allow both userlayer and namedlayer
            StyleLayer sl = new StyleLayer(si, styledLayers[0], gs.getCatalog());
            layers.add(sl);
        } else {
            // here we skip the UserLayer, as they should contain data inline, so they get skipped
            layers =
                    Arrays.stream(styledLayers)
                            .filter(sl -> sl instanceof NamedLayer)
                            .map(nl -> new StyleLayer(si, nl, gs.getCatalog()))
                            .collect(Collectors.toList());
        }
    }

    private Style getStyle(StyleInfo si, StyledLayer styledLayer) {
        if (styledLayer instanceof UserLayer) {
            return ((UserLayer) styledLayer).getUserStyles()[0];
        } else if (styledLayer instanceof NamedLayer) {
            return ((NamedLayer) styledLayer).getStyles()[0];
        } else {
            throw new IllegalArgumentException(
                    "Do not recognize styled layer "
                            + styledLayer
                            + " inside "
                            + si.prefixedName());
        }
    }

    private String getPointOfContact(StyleInfo si) {
        StyleMetadataInfo styleMetadataInfo =
                si.getMetadata().get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class);
        if (styleMetadataInfo != null && styleMetadataInfo.getPointOfContact() != null) {
            return styleMetadataInfo.getPointOfContact();
        }

        return null;
    }

    private String getTitle(StyledLayerDescriptor sld) {
        if (sld.getTitle() != null) {
            return sld.getTitle();
        }

        return getDescriptions(sld)
                .filter(d -> d.getTitle() != null)
                .map(d -> d.getTitle().toString())
                .findFirst()
                .orElse(null);
    }

    private String getDescription(StyledLayerDescriptor sld) {
        if (sld.getAbstract() != null) {
            return sld.getAbstract();
        }

        return getDescriptions(sld)
                .filter(d -> d.getAbstract() != null)
                .map(d -> d.getAbstract().toString())
                .findFirst()
                .orElse(null);
    }

    private List<String> getKeywords(StyleInfo si) throws IOException {
        StyleMetadataInfo styleMetadataInfo =
                si.getMetadata().get(StyleMetadataInfo.METADATA_KEY, StyleMetadataInfo.class);
        if (styleMetadataInfo != null && styleMetadataInfo.getAbstract() != null) {
            return styleMetadataInfo.getKeywords();
        }

        return null;
    }

    /** Extracts the description of all styles contained in user layers or named layers */
    private Stream<Description> getDescriptions(StyledLayerDescriptor sld) {
        return Arrays.stream(sld.getStyledLayers())
                .flatMap(
                        sl -> {
                            if (sl instanceof NamedLayer) {
                                return Arrays.stream(((NamedLayer) sl).getStyles());
                            } else if (sl instanceof UserLayer) {
                                return Arrays.stream(((UserLayer) sl).getUserStyles());
                            }
                            return Stream.empty();
                        })
                .filter(s -> s != null && s.getDescription() != null && s.getDescription() != null)
                .map(s -> s.getDescription());
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getPointOfContact() {
        return pointOfContact;
    }

    public String getAccessConstraints() {
        return accessConstraints;
    }

    public StyleDates getDates() {
        return dates;
    }

    public String getScope() {
        return scope;
    }

    public List<Stylesheet> getStylesheets() {
        return stylesheets;
    }

    public List<StyleLayer> getLayers() {
        return layers;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.titleSet = true;
        this.title = title;
    }

    public void setDescription(String description) {
        this.descriptionSet = true;
        this.description = description;
    }

    public void setKeywords(List<String> keywords) {
        this.keywordsSet = true;
        this.keywords = keywords;
    }

    public void setPointOfContact(String pointOfContact) {
        this.pointOfContactSet = true;
        this.pointOfContact = pointOfContact;
    }

    public void setAccessConstraints(String accessConstraints) {
        this.accessConstraintsSet = true;
        this.accessConstraints = accessConstraints;
    }

    public void setDates(StyleDates dates) {
        this.dates = dates;
    }

    @JsonIgnore
    public boolean isTitleSet() {
        return titleSet;
    }

    @JsonIgnore
    public boolean isDescriptionSet() {
        return descriptionSet;
    }

    @JsonIgnore
    public boolean isKeywordsSet() {
        return keywordsSet;
    }

    @JsonIgnore
    public boolean isPointOfContactSet() {
        return pointOfContactSet;
    }

    @JsonIgnore
    public boolean isAccessConstraintsSet() {
        return accessConstraintsSet;
    }

    @JsonIgnore
    public boolean isDatesSet() {
        return datesSet;
    }
}
