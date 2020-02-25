/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts;

import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geowebcache.config.meta.ServiceContact;
import org.geowebcache.config.meta.ServiceInformation;
import org.geowebcache.config.meta.ServiceProvider;
import org.geowebcache.service.wmts.WMTSExtensionImpl;

/** Makes WMTS service metadata configured in GeoServer available to GWC */
public class WMTSCapabilitiesProvider extends WMTSExtensionImpl {

    private final GeoServer geoserver;

    public WMTSCapabilitiesProvider(GeoServer geoserver) {
        this.geoserver = geoserver;
    }

    @Override
    public ServiceInformation getServiceInformation() {
        // make WMTS service metadata configured in GeoServer available to GWC
        WMTSInfo gsInfo = geoserver.getService(WMTSInfo.class);
        ServiceInformation gwcInfo = new ServiceInformation();
        // add service information
        gwcInfo.setTitle(gsInfo.getTitle());
        gwcInfo.setTitle(gsInfo.getTitle());
        gwcInfo.setDescription(gsInfo.getAbstract());
        gwcInfo.getKeywords().addAll(gsInfo.keywordValues());
        gwcInfo.setFees(gsInfo.getFees());
        gwcInfo.setAccessConstraints(gsInfo.getAccessConstraints());
        // add provider information
        ServiceProvider serviceProvider = new ServiceProvider();
        serviceProvider.setProviderName(gsInfo.getMaintainer());
        serviceProvider.setProviderName(gsInfo.getOnlineResource());
        // add contact information
        ContactInfo gsContactInfo = geoserver.getSettings().getContact();
        if (gsContactInfo != null) {
            ServiceContact gwcContactInfo = new ServiceContact();
            gwcContactInfo.setIndividualName(gsContactInfo.getContactPerson());
            gwcContactInfo.setPositionName(gsContactInfo.getContactPosition());
            gwcContactInfo.setAddressType(gsContactInfo.getAddressType());
            gwcContactInfo.setAddressStreet(gsContactInfo.getAddress());
            gwcContactInfo.setAddressCity(gsContactInfo.getAddressCity());
            gwcContactInfo.setAddressPostalCode(gsContactInfo.getAddressPostalCode());
            gwcContactInfo.setAddressCountry(gsContactInfo.getAddressCountry());
            gwcContactInfo.setPhoneNumber(gsContactInfo.getContactVoice());
            gwcContactInfo.setFaxNumber(gsContactInfo.getContactFacsimile());
            gwcContactInfo.setAddressEmail(gsContactInfo.getContactEmail());
            serviceProvider.setServiceContact(gwcContactInfo);
        }
        gwcInfo.setServiceProvider(serviceProvider);
        gwcInfo.setCiteCompliant(gsInfo.isCiteCompliant());
        return gwcInfo;
    }
}
