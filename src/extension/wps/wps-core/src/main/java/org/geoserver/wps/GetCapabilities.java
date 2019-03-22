/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import net.opengis.ows11.KeywordsType;
import net.opengis.ows11.OperationType;
import net.opengis.ows11.OperationsMetadataType;
import net.opengis.ows11.Ows11Factory;
import net.opengis.ows11.ResponsiblePartySubsetType;
import net.opengis.ows11.ServiceIdentificationType;
import net.opengis.ows11.ServiceProviderType;
import net.opengis.wps10.DefaultType2;
import net.opengis.wps10.GetCapabilitiesType;
import net.opengis.wps10.LanguagesType;
import net.opengis.wps10.LanguagesType1;
import net.opengis.wps10.ProcessBriefType;
import net.opengis.wps10.ProcessOfferingsType;
import net.opengis.wps10.WPSCapabilitiesType;
import net.opengis.wps10.Wps10Factory;
import org.eclipse.emf.common.util.ECollections;
import org.geoserver.config.SettingsInfo;
import org.geoserver.ows.Ows11Util;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.wps.process.GeoServerProcessors;
import org.geotools.process.ProcessFactory;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.context.ApplicationContext;

/** @author Lucas Reed, Refractions Research Inc */
public class GetCapabilities {
    public WPSInfo wps;

    ApplicationContext context;

    static final Logger LOGGER = Logging.getLogger(GetCapabilities.class);

    public GetCapabilities(WPSInfo wps, ApplicationContext context) {
        this.wps = wps;
        this.context = context;
    }

    public WPSCapabilitiesType run(GetCapabilitiesType request) throws WPSException {
        // do the version negotiation dance
        List<String> provided = Collections.singletonList("1.0.0");
        List<String> accepted = null;
        if (request.getAcceptVersions() != null)
            accepted = request.getAcceptVersions().getVersion();
        String version = RequestUtils.getVersionOws11(provided, accepted);

        if (!"1.0.0".equals(version)) {
            throw new WPSException("Could not understand version:" + version);
        }

        // TODO: add update sequence negotiation

        // encode the response
        Wps10Factory wpsf = Wps10Factory.eINSTANCE;
        Ows11Factory owsf = Ows11Factory.eINSTANCE;

        WPSCapabilitiesType caps = wpsf.createWPSCapabilitiesType();
        caps.setVersion("1.0.0");

        // TODO: make configurable
        caps.setLang("en");

        // ServiceIdentification
        ServiceIdentificationType si = owsf.createServiceIdentificationType();
        caps.setServiceIdentification(si);

        // Check if WPS config is loaded
        if (wps == null) {
            throw new ServiceException("WPS config not loaded. Check logs for details.");
        }

        si.getTitle().add(Ows11Util.languageString(wps.getTitle()));
        si.getAbstract().add(Ows11Util.languageString(wps.getAbstract()));

        KeywordsType kw = Ows11Util.keywords(wps.keywordValues());
        if (kw != null) {
            si.getKeywords().add(kw);
        }

        si.setServiceType(Ows11Util.code("WPS"));
        si.setServiceTypeVersion("1.0.0");
        si.setFees(wps.getFees());

        if (wps.getAccessConstraints() != null) {
            si.setAccessConstraints(wps.getAccessConstraints());
        }

        // ServiceProvider
        ServiceProviderType sp = owsf.createServiceProviderType();
        caps.setServiceProvider(sp);

        // TODO: set provder name from context
        SettingsInfo settings = wps.getGeoServer().getSettings();
        if (settings.getContact().getContactOrganization() != null) {
            sp.setProviderName(settings.getContact().getContactOrganization());
        } else {
            sp.setProviderName("GeoServer");
        }

        sp.setProviderSite(owsf.createOnlineResourceType());
        sp.getProviderSite().setHref(settings.getOnlineResource());
        sp.setServiceContact(responsibleParty(settings, owsf));

        // OperationsMetadata
        OperationsMetadataType om = owsf.createOperationsMetadataType();
        caps.setOperationsMetadata(om);

        OperationType gco = owsf.createOperationType();
        gco.setName("GetCapabilities");
        gco.getDCP().add(Ows11Util.dcp("wps", request));
        om.getOperation().add(gco);

        OperationType dpo = owsf.createOperationType();
        dpo.setName("DescribeProcess");
        dpo.getDCP().add(Ows11Util.dcp("wps", request));
        om.getOperation().add(dpo);

        OperationType eo = owsf.createOperationType();
        eo.setName("Execute");
        eo.getDCP().add(Ows11Util.dcp("wps", request));
        om.getOperation().add(eo);

        ProcessOfferingsType po = wpsf.createProcessOfferingsType();
        caps.setProcessOfferings(po);

        // gather the process list
        Set<ProcessFactory> pfs = GeoServerProcessors.getProcessFactories();
        for (ProcessFactory pf : pfs) {
            for (Name name : pf.getNames()) {
                ProcessBriefType p = wpsf.createProcessBriefType();
                p.setProcessVersion(pf.getVersion(name));
                po.getProcess().add(p);

                p.setIdentifier(Ows11Util.code(name));
                p.setTitle(Ows11Util.languageString(pf.getTitle(name)));
                p.setAbstract(Ows11Util.languageString(pf.getDescription(name)));
            }
        }
        // sort it
        ECollections.sort(
                po.getProcess(),
                new Comparator() {

                    public int compare(Object o1, Object o2) {
                        ProcessBriefType pb1 = (ProcessBriefType) o1;
                        ProcessBriefType pb2 = (ProcessBriefType) o2;

                        final String id1 = pb1.getIdentifier().getValue();
                        final String id2 = pb2.getIdentifier().getValue();
                        return id1.compareTo(id2);
                    }
                });

        LanguagesType1 languages = wpsf.createLanguagesType1();
        caps.setLanguages(languages);

        DefaultType2 defaultLanguage = wpsf.createDefaultType2();
        languages.setDefault(defaultLanguage);
        defaultLanguage.setLanguage("en-US");

        LanguagesType supportedLanguages = wpsf.createLanguagesType();
        languages.setSupported(supportedLanguages);
        supportedLanguages.getLanguage().add("en-US");

        return caps;
        // Version detection and alternative invocation if being implemented.
    }

    ResponsiblePartySubsetType responsibleParty(SettingsInfo settings, Ows11Factory f) {
        ResponsiblePartySubsetType rp = f.createResponsiblePartySubsetType();
        return rp;
    }
}
