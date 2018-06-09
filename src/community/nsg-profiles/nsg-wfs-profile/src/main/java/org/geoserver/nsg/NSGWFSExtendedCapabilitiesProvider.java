/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.nsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.nsg.timeout.TimeoutCallback;
import org.geoserver.wfs.DomainType;
import org.geoserver.wfs.GMLInfo;
import org.geoserver.wfs.OperationMetadata;
import org.geoserver.wfs.WFSExtendedCapabilitiesProvider;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.request.GetCapabilitiesRequest;
import org.geotools.util.Version;
import org.xml.sax.helpers.NamespaceSupport;

public class NSGWFSExtendedCapabilitiesProvider implements WFSExtendedCapabilitiesProvider {

    public static final String NSG_BASIC = "http://www.nga.mil/service/wfs/2.0/profile/basic";
    public static final String IMPLEMENTS_ENHANCED_PAGING = "ImplementsEnhancedPaging";
    public static final String IMPLEMENTS_FEATURE_VERSIONING = "ImplementsFeatureVersioning";
    static final Set<String> SRS_OPERATIONS =
            new HashSet<>(Arrays.asList("GetFeature", "GetFeatureWithLock", "Transaction"));
    static final Set<String> TIMEOUT_OPERATIONS =
            new HashSet<>(
                    Arrays.asList(
                            "GetFeature", "GetFeatureWithLock", "GetPropertyValue", "PageResults"));
    static final String GML32_FORMAT = "application/gml+xml; version=3.2";

    GeoServer gs;

    public NSGWFSExtendedCapabilitiesProvider(GeoServer gs) {
        this.gs = gs;
    }

    @Override
    public String[] getSchemaLocations(String schemaBaseURL) {
        return new String[0];
    }

    @Override
    public void registerNamespaces(NamespaceSupport namespaces) {
        // nothing to register
    }

    @Override
    public void encode(Translator tx, WFSInfo wfs, GetCapabilitiesRequest getCapabilitiesRequest)
            throws IOException {
        // no extensions to encode here
    }

    @Override
    public List<String> getProfiles(Version version) {
        if (isNSGProfileApplicable(version)) {
            return Arrays.asList(
                    NSG_BASIC
                    /*, "http://www.dgiwg.org/service/wfs/2.0/profile/locking" */ );
        } else {
            return Collections.emptyList();
        }
    }

    private boolean isNSGProfileApplicable(Version version) {
        return Integer.valueOf(2).equals(version.getMajor());
    }

    @Override
    public void updateRootOperationConstraints(Version version, List<DomainType> constraints) {
        if (isNSGProfileApplicable(version)) {
            for (DomainType constraint : constraints) {
                if (IMPLEMENTS_FEATURE_VERSIONING.equals(constraint.getName())) {
                    constraint.setDefaultValue("TRUE");
                }
            }
        }
        constraints.add(new DomainType(IMPLEMENTS_ENHANCED_PAGING, "TRUE"));
    }

    @Override
    public void updateOperationMetadata(Version version, List<OperationMetadata> operations) {
        if (isNSGProfileApplicable(version)) {
            // prepare SRS customization
            WFSInfo wfs = gs.getService(WFSInfo.class);
            DomainType srsParameter = getSrsParameter(wfs);
            DomainType timeoutParameter = getTimeoutParameter(wfs);
            DomainType versionParameter =
                    new DomainType("version", Arrays.asList("2.0.0", "1.1.0", "1.0.0"));

            // add the paged results operation
            OperationMetadata pageResults = new OperationMetadata("PageResults", true, true);
            pageResults.getParameters().add(new DomainType("outputFormat", GML32_FORMAT));
            operations.add(pageResults);

            for (OperationMetadata operation : operations) {
                // add the version if not GetCapabilities, could have been done in core, but this
                // seems like a niche
                // nitpick, it's a WFS 2.0 capabilities the version number should be obvious to the
                // client
                if (!"GetCapabilities".equals(operation.getName())) {
                    // add the version if missing
                    if (!containsParameter(operation, "version")) {
                        operation.getParameters().add(versionParameter);
                    }
                }
                // add the srs if configured in WFS
                if (SRS_OPERATIONS.contains(operation.getName())
                        && !containsParameter(operation, srsParameter.getName())) {
                    operation.getParameters().add(srsParameter);
                }
                // add the timeout if configured in WFS
                if (TIMEOUT_OPERATIONS.contains(operation.getName())
                        && !containsParameter(operation, timeoutParameter.getName())) {
                    operation.getParameters().add(timeoutParameter);
                }
            }
        }
    }

    private DomainType getTimeoutParameter(WFSInfo wfs) {
        Integer timeout = wfs.getMetadata().get(TimeoutCallback.TIMEOUT_CONFIG_KEY, Integer.class);
        if (timeout == null) {
            timeout = 300;
        }
        DomainType result = new DomainType("Timeout", String.valueOf(timeout));
        return result;
    }

    public DomainType getSrsParameter(WFSInfo wfs) {
        List<String> extraSRS = wfs.getSRS();
        Set<String> srsParameterValues;
        GMLInfo gml = wfs.getGML().get(WFSInfo.Version.V_20);
        String prefix = gml.getSrsNameStyle().getPrefix();
        Function<String, String> epsgMapper = srs -> qualifySRS(prefix, srs);
        if (extraSRS != null && !extraSRS.isEmpty()) {
            srsParameterValues =
                    extraSRS.stream()
                            .map(epsgMapper)
                            .collect(Collectors.toCollection(LinkedHashSet::new));
        } else {
            srsParameterValues = new LinkedHashSet<>();
        }
        // add values from feature types
        gs.getCatalog()
                .getFeatureTypes()
                .forEach(
                        ft -> {
                            String srs = epsgMapper.apply(ft.getSRS());
                            srsParameterValues.add(srs);
                        });

        // build the parameter
        DomainType srsParameter = new DomainType("srsName", new ArrayList<>(srsParameterValues));
        return srsParameter;
    }

    public String qualifySRS(String prefix, String srs) {
        if (srs.matches("(?ui)EPSG:[0-9]+")) {
            srs = prefix + srs.substring(5);
        } else {
            srs = prefix + srs;
        }
        return srs;
    }

    private boolean containsParameter(OperationMetadata operation, String parameterName) {
        return operation.getParameters().stream().anyMatch(p -> parameterName.equals(p.getName()));
    }
}
