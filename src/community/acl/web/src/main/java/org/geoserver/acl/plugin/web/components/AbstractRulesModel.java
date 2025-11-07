/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.components;

import com.google.common.collect.Streams;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.Service;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.web.GeoServerApplication;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;

@SuppressWarnings("serial")
public abstract class AbstractRulesModel implements Serializable {

    private static final Logger log = Logging.getLogger(AbstractRulesModel.class);

    /**
     * Maximum number of items to show on {@link #getWorkspaceChoices(}, {@link #getUserChoices},
     * {@link #getRoleChoices}
     */
    protected static final int MAX_SUGGESTIONS = 100;

    private Stream<Service> findServices() {
        return GeoServerExtensions.extensions(org.geoserver.platform.Service.class).stream();
    }

    public List<String> findServiceNames() {
        return Stream.concat(
                        KNOWN_SERVICES.keySet().stream(),
                        findServices().map(Service::getId).map(String::toUpperCase))
                .sorted()
                .distinct()
                .filter(service -> !"GWC".equalsIgnoreCase(service))
                .collect(Collectors.toList());
    }

    /** Returns a sorted list of operation names in the specified {@link Service#getId() service name} */
    public List<String> findOperationNames(String serviceName) {
        if (!StringUtils.hasText(serviceName)) return List.of();

        Stream<String> knownOps = KNOWN_SERVICES.getOrDefault(serviceName, List.of()).stream();

        return Stream.concat(
                        knownOps,
                        findServices()
                                .filter(s -> s.getId().equalsIgnoreCase(serviceName))
                                .findFirst()
                                .map(Service::getOperations)
                                .map(List::stream)
                                .orElseGet(Stream::empty))
                .sorted()
                .distinct()
                .collect(Collectors.toList());
    }

    public Iterator<String> getSubfieldChoices(@Nullable String input) {
        final Pattern test = caseInsensitiveContains(input);
        return KNOWN_WPS_PROCESSES.stream()
                .filter(process -> inputMatches(test, process))
                .iterator();
    }

    /** Supports {@link #getUserChoices(String)} in getting the selected role for the current model */
    protected abstract String getSelectedRoleName();

    public Iterator<String> getUserChoices(String input) {
        final Pattern test = caseInsensitiveStartsWith(input);
        final String roleName = getSelectedRoleName();
        return getUserNamesByRole(roleName)
                .filter(user -> inputMatches(test, user))
                .sorted()
                .distinct()
                .limit(MAX_SUGGESTIONS)
                .iterator();
    }

    public Iterator<String> getRoleChoices(@Nullable String input) {
        final Pattern test = caseInsensitiveStartsWith(input);
        return getAvailableRoles()
                .filter(role -> inputMatches(test, role))
                .limit(MAX_SUGGESTIONS)
                .iterator();
    }

    public Iterator<String> getWorkspaceChoices(@Nullable String input) {
        final Pattern test = caseInsensitiveStartsWith(input);
        return getWorkspaceNames()
                .filter(workspace -> inputMatches(test, workspace))
                .limit(MAX_SUGGESTIONS)
                .iterator();
    }

    public Iterator<PublishedInfo> getLayerChoices(@Nullable String input) {
        String workspace = getSelectedWorkspace();
        workspace = nonNull(workspace);
        final String test = nonNull(input);

        Stream<PublishedInfo> options;

        final Catalog rawCatalog = rawCatalog();
        if (StringUtils.hasText(workspace)) {
            String prefixedSearch = workspace + ":" + test;
            Filter filter = Predicates.contains("prefixedName", prefixedSearch);
            SortBy sortByName = Predicates.sortBy("name", true);

            // REVISIT: check if it's actually closed
            try (CloseableIterator<PublishedInfo> it =
                    rawCatalog.list(PublishedInfo.class, filter, 0, MAX_SUGGESTIONS, sortByName)) {
                options = Streams.stream(it)
                        .filter(PublishedInfo::isAdvertised)
                        .filter(PublishedInfo::isEnabled)
                        .limit(MAX_SUGGESTIONS)
                        .collect(Collectors.toList())
                        .stream();
            }
        } else {
            options = rawCatalog.getLayerGroupsByWorkspace(CatalogFacade.NO_WORKSPACE).stream()
                    .filter(PublishedInfo::isAdvertised)
                    .filter(PublishedInfo::isEnabled)
                    .map(PublishedInfo.class::cast)
                    .sorted((g1, g2) -> g1.getName().compareTo(g2.getName()))
                    .limit(MAX_SUGGESTIONS);
        }

        return options.iterator();
    }

    /** Supports getting the current workspace for {@link #getLayerChoices(String)} in the specific model */
    protected abstract String getSelectedWorkspace();

    protected Pattern caseInsensitiveStartsWith(@Nullable String input) {
        return Pattern.compile(Pattern.quote(nonNull(input)) + ".*", Pattern.CASE_INSENSITIVE);
    }

    protected Pattern caseInsensitiveContains(@Nullable String input) {
        return Pattern.compile(".*" + Pattern.quote(nonNull(input)) + ".*", Pattern.CASE_INSENSITIVE);
    }

    protected Pattern startsWith(@NonNull String input) {
        return Pattern.compile(Pattern.quote(nonNull(input)) + ".*");
    }

    protected String nonNull(String input) {
        return StringUtils.hasText(input) ? input.trim() : "";
    }

    protected boolean inputMatches(@NonNull Pattern input, @NonNull String choice) {
        return input.matcher(choice).matches();
    }

    protected Stream<String> getWorkspaceNames() {
        return rawCatalog().getWorkspaces().stream()
                .parallel()
                .map(WorkspaceInfo::getName)
                .sorted();
    }

    protected Stream<String> getAvailableRoles() {
        try {
            SortedSet<GeoServerRole> rolesForAccessControl = securityManager().getRolesForAccessControl();
            return Stream.concat(Stream.of(GeoServerRole.ADMIN_ROLE), rolesForAccessControl.stream())
                    .map(GeoServerRole::getAuthority)
                    .sorted()
                    .distinct();
        } catch (IOException e) {
            log.log(Level.WARNING, "Error obtaining available roles", e);
            return Stream.empty();
        }
    }

    protected Stream<String> getUserNamesByRole(String roleName) {

        GeoServerSecurityManager securityManager = securityManager();
        try {
            if (StringUtils.hasText(roleName)) {
                SortedSet<String> ret = new TreeSet<>();
                for (String serviceName : securityManager.listRoleServices()) {
                    GeoServerRoleService roleService = securityManager.loadRoleService(serviceName);
                    GeoServerRole role = roleService.getRoleByName(roleName);
                    if (role != null) {
                        SortedSet<String> usernames = roleService.getUserNamesForRole(role);
                        ret.addAll(usernames);
                    }
                }
                return ret.stream();
            }

            return securityManager.loadUserGroupServices().stream()
                    .map(t -> {
                        try {
                            return t.getUsers();
                        } catch (IOException e) {
                            log.log(Level.WARNING, "Error getting users from group service " + t.getName(), e);
                            return Set.<GeoServerUser>of();
                        }
                    })
                    .flatMap(Set::stream)
                    .map(GeoServerUser::getUsername)
                    .sorted()
                    .distinct();
        } catch (IOException e) {
            log.log(Level.WARNING, "Error getting users for role " + roleName, e);
            return Stream.empty();
        }
    }

    public Stream<String> findUserRoles(String userName) {
        Set<GeoServerRole> rolesForUser = Set.of();
        if (StringUtils.hasText(userName)) {
            GeoServerSecurityManager securityManager = securityManager();
            GeoServerRoleService roleService = securityManager.getActiveRoleService();
            try {
                rolesForUser = roleService.getRolesForUser(userName);
            } catch (IOException e) {
                log.log(Level.WARNING, "Error fetching roles for user %s: %s".formatted(userName, e.getMessage()), e);
            }
        }
        return rolesForUser.stream().map(GeoServerRole::getAuthority);
    }

    public Catalog rawCatalog() {
        return (Catalog) GeoServerExtensions.bean("rawCatalog");
    }

    protected GeoServerSecurityManager securityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }

    protected static final Map<String, List<String>> KNOWN_SERVICES = Map.of(
            "WMS",
            List.of("GetCapabilities", "GetMap", "DescribeLayer", "GetFeatureInfo", "GetLegendGraphic", "GetStyles"),
            "WFS",
            List.of(
                    "GetCapabilities",
                    "GetFeature",
                    "DescribeFeatureType",
                    "LockFeature",
                    "GetFeatureWithLock",
                    "Transaction",
                    // WFS 1.1 additional operations:
                    "GetGMLObject",
                    // WFS 2.0 additional operations:
                    "GetPropertyValue",
                    "GetFeatureWithLock",
                    "CreateStoredQuery",
                    "DropStoredQuery",
                    "ListStoredQueries",
                    "DescribeStoredQueries"),
            "WCS",
            List.of("GetCapabilities", "GetCoverage", "DescribeCoverage"),
            "WPS",
            List.of("GetCapabilities", "DescribeProcess", "Execute"));

    protected static final List<String> KNOWN_WPS_PROCESSES = List.of(
            "JTS:area",
            "JTS:boundary",
            "JTS:buffer",
            "JTS:centroid",
            "JTS:contains",
            "JTS:convexHull",
            "JTS:crosses",
            "JTS:densify",
            "JTS:difference",
            "JTS:dimension",
            "JTS:disjoint",
            "JTS:distance",
            "JTS:endPoint",
            "JTS:envelope",
            "JTS:equalsExact",
            "JTS:equalsExactTolerance",
            "JTS:exteriorRing",
            "JTS:geometryType",
            "JTS:getGeometryN",
            "JTS:getX",
            "JTS:getY",
            "JTS:interiorPoint",
            "JTS:interiorRingN",
            "JTS:intersection",
            "JTS:intersects",
            "JTS:isClosed",
            "JTS:isEmpty",
            "JTS:isRing",
            "JTS:isSimple",
            "JTS:isValid",
            "JTS:isWithinDistance",
            "JTS:length",
            "JTS:numGeometries",
            "JTS:numInteriorRing",
            "JTS:numPoints",
            "JTS:overlaps",
            "JTS:pointN",
            "JTS:polygonize",
            "JTS:relate",
            "JTS:relatePattern",
            "JTS:reproject",
            "JTS:simplify",
            "JTS:splitPolygon",
            "JTS:startPoint",
            "JTS:symDifference",
            "JTS:touches",
            "JTS:union",
            "JTS:within",
            "centerLine:centerLine",
            "geo:area",
            "geo:boundary",
            "geo:buffer",
            "geo:centroid",
            "geo:contains",
            "geo:convexHull",
            "geo:crosses",
            "geo:densify",
            "geo:difference",
            "geo:dimension",
            "geo:disjoint",
            "geo:distance",
            "geo:endPoint",
            "geo:envelope",
            "geo:equalsExact",
            "geo:equalsExactTolerance",
            "geo:exteriorRing",
            "geo:geometryType",
            "geo:getGeometryN",
            "geo:getX",
            "geo:getY",
            "geo:interiorPoint",
            "geo:interiorRingN",
            "geo:intersection",
            "geo:intersects",
            "geo:isClosed",
            "geo:isEmpty",
            "geo:isRing",
            "geo:isSimple",
            "geo:isValid",
            "geo:isWithinDistance",
            "geo:length",
            "geo:numGeometries",
            "geo:numInteriorRing",
            "geo:numPoints",
            "geo:overlaps",
            "geo:pointN",
            "geo:polygonize",
            "geo:relate",
            "geo:relatePattern",
            "geo:reproject",
            "geo:simplify",
            "geo:splitPolygon",
            "geo:startPoint",
            "geo:symDifference",
            "geo:touches",
            "geo:union",
            "geo:within",
            "gs:AddCoverages",
            "gs:Aggregate",
            "gs:AreaGrid",
            "gs:BarnesSurface",
            "gs:Bounds",
            "gs:BufferFeatureCollection",
            "gs:Centroid",
            "gs:Clip",
            "gs:CollectGeometries",
            "gs:Contour",
            "gs:Count",
            "gs:CropCoverage",
            "gs:Feature",
            "gs:GeorectifyCoverage",
            "gs:GetFullCoverage",
            "gs:Grid",
            "gs:Heatmap",
            "gs:Import",
            "gs:InclusionFeatureCollection",
            "gs:IntersectionFeatureCollection",
            "gs:LRSGeocode",
            "gs:LRSMeasure",
            "gs:LRSSegment",
            "gs:MultiplyCoverages",
            "gs:Nearest",
            "gs:PagedUnique",
            "gs:PointBuffers",
            "gs:PointStacker",
            "gs:PolygonExtraction",
            "gs:Query",
            "gs:RangeLookup",
            "gs:RasterAsPointCollection",
            "gs:RasterZonalStatistics",
            "gs:RectangularClip",
            "gs:Reproject",
            "gs:ReprojectGeometry",
            "gs:ScaleCoverage",
            "gs:Simplify",
            "gs:Snap",
            "gs:StoreCoverage",
            "gs:StyleCoverage",
            "gs:Transform",
            "gs:UnionFeatureCollection",
            "gs:Unique",
            "gs:VectorZonalStatistics",
            "gt:VectorToRaster",
            "polygonlabelprocess:PolyLabeller",
            "ras:AddCoverages",
            "ras:Affine",
            "ras:AreaGrid",
            "ras:BandMerge",
            "ras:BandSelect",
            "ras:Contour",
            "ras:CoverageClassStats",
            "ras:CropCoverage",
            "ras:Jiffle",
            "ras:MultiplyCoverages",
            "ras:NormalizeCoverage",
            "ras:PolygonExtraction",
            "ras:RangeLookup",
            "ras:RasterAsPointCollection",
            "ras:RasterZonalStatistics",
            "ras:ScaleCoverage",
            "ras:StyleCoverage",
            "ras:TransparencyFill",
            "skeltonize:centerLine",
            "vec:Aggregate",
            "vec:BarnesSurface",
            "vec:Bounds",
            "vec:BufferFeatureCollection",
            "vec:Centroid",
            "vec:ClassifyByRange",
            "vec:Clip",
            "vec:CollectGeometries",
            "vec:Count",
            "vec:Feature",
            "vec:FeatureClassStats",
            "vec:Grid",
            "vec:Heatmap",
            "vec:InclusionFeatureCollection",
            "vec:IntersectionFeatureCollection",
            "vec:LRSGeocode",
            "vec:LRSMeasure",
            "vec:LRSSegment",
            "vec:Nearest",
            "vec:PointBuffers",
            "vec:PointStacker",
            "vec:Query",
            "vec:RectangularClip",
            "vec:Reproject",
            "vec:Simplify",
            "vec:Snap",
            "vec:Transform",
            "vec:UnionFeatureCollection",
            "vec:Unique",
            "vec:VectorToRaster",
            "vec:VectorZonalStatistics");
}
