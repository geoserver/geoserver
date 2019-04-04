/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.xml.v1_0;

import net.opengis.ows10.Ows10Factory;
import org.geotools.xlink.XLINKConfiguration;
import org.geotools.xsd.Configuration;
import org.picocontainer.MutablePicoContainer;

/**
 * Parser configuration for ows schema.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class OWSConfiguration extends Configuration {
    /** Creates a new configuration, adding the dependency on {@link OWSConfiguration}. */
    public OWSConfiguration() {
        super(OWS.getInstance());

        addDependency(new XLINKConfiguration());
    }

    protected void registerBindings(MutablePicoContainer container) {
        // Types
        container.registerComponentImplementation(
                OWS.ACCEPTFORMATSTYPE, AcceptFormatsTypeBinding.class);
        container.registerComponentImplementation(
                OWS.ACCEPTVERSIONSTYPE, AcceptVersionsTypeBinding.class);
        container.registerComponentImplementation(OWS.ADDRESSTYPE, AddressTypeBinding.class);
        container.registerComponentImplementation(
                OWS.BOUNDINGBOXTYPE, BoundingBoxTypeBinding.class);
        container.registerComponentImplementation(
                OWS.CAPABILITIESBASETYPE, CapabilitiesBaseTypeBinding.class);
        container.registerComponentImplementation(OWS.CODETYPE, CodeTypeBinding.class);
        container.registerComponentImplementation(OWS.CONTACTTYPE, ContactTypeBinding.class);
        container.registerComponentImplementation(
                OWS.DESCRIPTIONTYPE, DescriptionTypeBinding.class);
        container.registerComponentImplementation(OWS.DOMAINTYPE, DomainTypeBinding.class);
        container.registerComponentImplementation(OWS.EXCEPTIONTYPE, ExceptionTypeBinding.class);
        container.registerComponentImplementation(
                OWS.GETCAPABILITIESTYPE, GetCapabilitiesTypeBinding.class);
        container.registerComponentImplementation(
                OWS.IDENTIFICATIONTYPE, IdentificationTypeBinding.class);
        container.registerComponentImplementation(OWS.KEYWORDSTYPE, KeywordsTypeBinding.class);
        container.registerComponentImplementation(OWS.METADATATYPE, MetadataTypeBinding.class);
        // container.registerComponentImplementation(OWS.MIMETYPE,MimeTypeBinding.class);
        container.registerComponentImplementation(
                OWS.ONLINERESOURCETYPE, OnlineResourceTypeBinding.class);
        container.registerComponentImplementation(OWS.POSITIONTYPE, PositionTypeBinding.class);
        container.registerComponentImplementation(OWS.POSITIONTYPE2D, PositionType2DBinding.class);
        container.registerComponentImplementation(
                OWS.REQUESTMETHODTYPE, RequestMethodTypeBinding.class);
        container.registerComponentImplementation(
                OWS.RESPONSIBLEPARTYSUBSETTYPE, ResponsiblePartySubsetTypeBinding.class);
        container.registerComponentImplementation(
                OWS.RESPONSIBLEPARTYTYPE, ResponsiblePartyTypeBinding.class);
        container.registerComponentImplementation(OWS.SECTIONSTYPE, SectionsTypeBinding.class);
        // container.registerComponentImplementation(OWS.SERVICETYPE,ServiceTypeBinding.class);
        container.registerComponentImplementation(OWS.TELEPHONETYPE, TelephoneTypeBinding.class);
        // container.registerComponentImplementation(OWS.UPDATESEQUENCETYPE,UpdateSequenceTypeBinding.class);
        // container.registerComponentImplementation(OWS.VERSIONTYPE,VersionTypeBinding.class);
        container.registerComponentImplementation(
                OWS.WGS84BOUNDINGBOXTYPE, WGS84BoundingBoxTypeBinding.class);

        // elements
        container.registerComponentImplementation(
                OWS.EXCEPTIONREPORT, ExceptionReportBinding.class);
    }

    /**
     * Configures the ows context.
     *
     * <p>The following factories are registered:
     *
     * <ul>
     *   <li>{@link Ows10Factory}
     * </ul>
     */
    protected void configureContext(MutablePicoContainer container) {
        super.configureContext(container);

        container.registerComponentInstance(Ows10Factory.eINSTANCE);
    }
}
