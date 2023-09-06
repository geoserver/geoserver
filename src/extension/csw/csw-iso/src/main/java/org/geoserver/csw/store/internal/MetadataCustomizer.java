/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.csw.DownloadLinkHandler;
import org.geoserver.csw.records.iso.MetaDataDescriptor;
import org.geotools.api.data.CloseableIterator;
import org.geotools.api.feature.ComplexAttribute;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.ComplexType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.feature.AttributeImpl;
import org.geotools.feature.ComplexAttributeImpl;

/** {@link FeatureCustomizer} subclass to deal with ISO Metadata type */
public class MetadataCustomizer extends FeatureCustomizer {

    private static final String TYPENAME = "MD_Metadata_Type";

    private static final String ONLINE_PARENT_NODE =
            "gmd:distributionInfo/gmd:MD_Distribution/gmd:transferOptions/gmd:MD_DigitalTransferOptions";

    private static final AttributeDescriptor ONLINE_DESCRIPTOR;

    private static final AttributeDescriptor LINKAGE_ATTRIBUTE_DESCRIPTOR;

    private static final AttributeDescriptor LINKAGE_URL_ATTRIBUTE_DESCRIPTOR;

    private static final AttributeDescriptor ONLINE_RESOURCE_DESCRIPTOR;

    static {
        ComplexType distrPropType =
                (ComplexType)
                        ((FeatureType) MetaDataDescriptor.METADATA_DESCRIPTOR.getType())
                                .getDescriptor("distributionInfo")
                                .getType();
        ComplexType distrType =
                (ComplexType) distrPropType.getDescriptor("MD_Distribution").getType();
        ComplexType transferOptionsPropType =
                (ComplexType) distrType.getDescriptor("transferOptions").getType();
        ComplexType transferOptionsType =
                (ComplexType)
                        transferOptionsPropType
                                .getDescriptor("MD_DigitalTransferOptions")
                                .getType();
        ONLINE_DESCRIPTOR = (AttributeDescriptor) transferOptionsType.getDescriptor("onLine");
        ComplexType onlinePropType = (ComplexType) ONLINE_DESCRIPTOR.getType();
        ONLINE_RESOURCE_DESCRIPTOR =
                (AttributeDescriptor) onlinePropType.getDescriptor("CI_OnlineResource");
        ComplexType onlineType = (ComplexType) ONLINE_RESOURCE_DESCRIPTOR.getType();
        LINKAGE_ATTRIBUTE_DESCRIPTOR = (AttributeDescriptor) onlineType.getDescriptor("linkage");
        ComplexType urlPropType = (ComplexType) LINKAGE_ATTRIBUTE_DESCRIPTOR.getType();
        LINKAGE_URL_ATTRIBUTE_DESCRIPTOR = (AttributeDescriptor) urlPropType.getDescriptor("URL");
    }

    /** An instance of {@link DownloadLinkHandler}, used to deal with download links */
    private DownloadLinkHandler downloadLinkHandler;

    public void setDownloadLinkHandler(DownloadLinkHandler downloadLinkHandler) {
        this.downloadLinkHandler = downloadLinkHandler;
    }

    public MetadataCustomizer() {
        super(TYPENAME);
    }

    @Override
    public void customizeFeature(Feature feature, CatalogInfo resource) {
        PropertyName parentPropertyName =
                ff.property(ONLINE_PARENT_NODE, MetaDataDescriptor.NAMESPACES);
        Property parentProperty = (Property) parentPropertyName.evaluate(feature);
        if (parentProperty == null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "Unable to get the specified property for the current feature: "
                                + ONLINE_PARENT_NODE
                                + "\n No customization will be applied");
            }
            return;
        }

        // Getting feature values to be updated
        Object value = parentProperty.getValue();
        if (value == null || !(value instanceof Collection)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        "Unable to get a value for the current property: "
                                + parentPropertyName.getPropertyName()
                                + "\n No customization will be applied");
            }
            return;
        }
        @SuppressWarnings("unchecked")
        Collection<ComplexAttribute> onlineValues = (Collection<ComplexAttribute>) value;

        // Copy the collection due to the immutable return
        Collection<ComplexAttribute> updatedOnlineResources =
                new ArrayList<>((Collection<ComplexAttribute>) onlineValues);

        // Invoke the DownloadLinkGenerator to generate links for the specified resource
        String link = null;
        try (CloseableIterator<String> links =
                downloadLinkHandler.generateDownloadLinks(resource)) {
            if (links != null) {
                while (links.hasNext()) {
                    link = links.next();
                    updatedOnlineResources.add(createOnlineResourceElement(link));
                }

                // link String should contain the last link generated
                // let's recycle it to generate the full download link
                updatedOnlineResources.add(
                        createOnlineResourceElement(
                                downloadLinkHandler.extractFullDownloadLink(link)));
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to close download links.", e);
        }
        // Update the onlineResources
        parentProperty.setValue(updatedOnlineResources);
    }

    /** Create a new OnlineResource element for the link */
    private ComplexAttribute createOnlineResourceElement(String link) {
        // Setting new URL attribute
        Property urlAttribute = new AttributeImpl(link, LINKAGE_URL_ATTRIBUTE_DESCRIPTOR, null);

        // Setting linkageURL
        Property linkage =
                new ComplexAttributeImpl(
                        Collections.singletonList(urlAttribute),
                        LINKAGE_ATTRIBUTE_DESCRIPTOR,
                        null);

        // Wrap in Online Resource
        Property onlineResource =
                new ComplexAttributeImpl(
                        Collections.singletonList(linkage), ONLINE_RESOURCE_DESCRIPTOR, null);

        // Wrap in onLine
        return new ComplexAttributeImpl(
                Collections.singletonList(onlineResource), ONLINE_DESCRIPTOR, null);
    }
}
