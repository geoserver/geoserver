/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
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
import org.geoserver.config.GeoServerInfo;
import org.geoserver.ows.Ows11Util;
import org.geoserver.ows.util.RequestUtils;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geotools.data.Parameter;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.springframework.context.ApplicationContext;

/**
 * @author Lucas Reed, Refractions Research Inc
 */
public class GetCapabilities {
    public WPSInfo wps;

    ApplicationContext context;

    static final Logger LOGGER = Logging.getLogger(GetCapabilities.class);

    private static Set<Name> PROCESS_BLACKLIST = Collections.EMPTY_SET;

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

        si.getTitle().add(Ows11Util.languageString(wps.getTitle()));
        si.getAbstract().add(Ows11Util.languageString(wps.getAbstract()));

        KeywordsType kw = Ows11Util.keywords(wps.getKeywords());
        ;
        if (kw != null) {
            si.getKeywords().add(kw);
        }

        si.setServiceType(Ows11Util.code("WPS"));
        si.getServiceTypeVersion().add("1.0.0");
        si.setFees(wps.getFees());

        if (wps.getAccessConstraints() != null) {
            si.getAccessConstraints().add(wps.getAccessConstraints());
        }

        // ServiceProvider
        ServiceProviderType sp = owsf.createServiceProviderType();
        caps.setServiceProvider(sp);

        // TODO: set provder name from context
        GeoServerInfo geoServer = wps.getGeoServer().getGlobal();
        if (geoServer.getContact().getContactOrganization() != null) {
            sp.setProviderName(geoServer.getContact().getContactOrganization());
        } else {
            sp.setProviderName("GeoServer");
        }

        sp.setProviderSite(owsf.createOnlineResourceType());
        sp.getProviderSite().setHref(geoServer.getOnlineResource());
        sp.setServiceContact(responsibleParty(geoServer, owsf));

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
        for (ProcessFactory pf : Processors.getProcessFactories()) {
            for (Name name : pf.getNames()) {
                if (!getProcessBlacklist().contains(name)) {
                    ProcessBriefType p = wpsf.createProcessBriefType();
                    p.setProcessVersion(pf.getVersion(name));
                    po.getProcess().add(p);

                    p.setIdentifier(Ows11Util.code(name));
                    p.setTitle(Ows11Util.languageString(pf.getTitle(name)));
                    p.setAbstract(Ows11Util.languageString(pf.getDescription(name)));
                }
            }
        }
        // sort it
        ECollections.sort(po.getProcess(), new Comparator() {

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

    /**
     * Checks if our WPS can really handle this process inputs and outputs
     * 
     * @param pf
     * @param name
     * @return
     */
    Set<Name> getProcessBlacklist() {
        synchronized (PROCESS_BLACKLIST) {
            if (PROCESS_BLACKLIST == Collections.EMPTY_SET) {

                Set<Name> blacklist = new HashSet<Name>();

                for (ProcessFactory pf : Processors.getProcessFactories()) {
                    int count = 0;
                    for (Name name : pf.getNames()) {
                        try {
                            // check inputs
                            for (Parameter<?> p : pf.getParameterInfo(name).values()) {
                                List<ProcessParameterIO> ppios = ProcessParameterIO.findAll(p, context);
                                if (ppios.isEmpty()) {
                                    LOGGER.log(Level.INFO, "Blacklisting process " + name.getURI()
                                            + " as the input " + p.key + " of type " + p.type
                                            + " cannot be handled");
                                    blacklist.add(name);
                                }
                            }
    
                            // check outputs
                            for (Parameter<?> p : pf.getResultInfo(name, null).values()) {
                                List<ProcessParameterIO> ppios = ProcessParameterIO.findAll(p, context);
                                if (ppios.isEmpty()) {
                                    LOGGER.log(Level.INFO, "Blacklisting process " + name.getURI()
                                            + " as the output " + p.key + " of type " + p.type
                                            + " cannot be handled");
                                    blacklist.add(name);
                                }
                            }
                        } catch(Throwable t) {
                            blacklist.add(name);
                        }
                        
                        if(!blacklist.contains(name)) {
                            count++;
                        }
                    }
                    LOGGER.info("Found " + count + " bindable processes in " + pf.getTitle());
                }
                

                PROCESS_BLACKLIST = blacklist;
            }
        }

        return PROCESS_BLACKLIST;
    }

    ResponsiblePartySubsetType responsibleParty(GeoServerInfo global, Ows11Factory f) {
        ResponsiblePartySubsetType rp = f.createResponsiblePartySubsetType();
        return rp;
    }
}