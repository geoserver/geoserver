/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.util.List;
import java.util.Set;
import org.geoserver.catalog.PublishedInfo;
import org.geotools.util.NumberRange;

/**
 * Extension point that allows plugins to dynamically contribute extended properties to the WMS
 * capabilities document.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface ExtendedCapabilitiesProvider
        extends org.geoserver.ExtendedCapabilitiesProvider<WMSInfo, GetCapabilitiesRequest> {

    /**
     * Returns the element names that are direct children of {@code VendorSpecificCapabilities}
     * contributed by this extended capabilities provider for WMS 1.1.1 DOCTYPE declaration.
     *
     * <p>This method returns only the element names that are direct children of
     * VendorSpecificCapabilities so that they can be aggregated in a single declaration like {@code
     * <!ELEMENT VendorSpecificCapabilities (ContributedElementOne*, ContributedElementTwo*) >} .
     * Implement {@link #getVendorSpecificCapabilitiesChildDecls(GetCapabilitiesRequest)} to
     * contribute the child elements of these root ones.
     *
     * @return the name of the elements to be declared as direct children of
     *     VendorSpecificCapabilities in a WMS 1.1.1 DOCTYPE internal DTD.
     */
    List<String> getVendorSpecificCapabilitiesRoots(GetCapabilitiesRequest request);

    /**
     * Returns the list of internal DTD element declarations contributed to WMS 1.1.1 DOCTYPE
     * GetCapabilities document.
     *
     * <p>Example DTD element declaration that could be a member of the returned list: " {@code
     * <!ELEMENT Resolutions (#PCDATA) >}"
     *
     * @return the list of GetCapabilities internal DTD elements declarations, may be empty.
     */
    List<String> getVendorSpecificCapabilitiesChildDecls(GetCapabilitiesRequest request);

    /**
     * Allows the provider to customize the srs list. For example, it can be used to provide a user
     * specific srs list
     */
    void customizeRootCrsList(Set<String> srs);

    /**
     * Allows the provider to customize the layer scale range, this can be used to advertise limited
     * visibility of the layer on a user by users basis.
     */
    NumberRange<Double> overrideScaleDenominators(
            PublishedInfo layer, NumberRange<Double> scaleDenominators);
}
