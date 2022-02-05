package org.geoserver.restconfig.api.v1.mapper;

import org.geoserver.openapi.model.catalog.CoverageInfo;
import org.geoserver.openapi.model.catalog.CoverageStoreInfo;
import org.geoserver.openapi.model.catalog.WorkspaceInfo;
import org.geoserver.openapi.v1.model.CoverageResponse;
import org.geoserver.openapi.v1.model.NamedLink;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface CoverageResponseMapper extends ResourceResponseMapper {

    @Mapping(source = "dimensions.coverageDimension", target = "dimensions")
    @Mapping(
            target = "nativeCoverageName",
            source = "nativeCoverageName", //
            defaultExpression =
                    "java(source.getParameters() == null? null : (String)source.getParameters().get(\"nativeCoverageName\"))")
    CoverageInfo map(CoverageResponse source);

    public default CoverageStoreInfo namedLinkToCoverageStoreInfo(NamedLink namedLink) {
        if (namedLink == null) return null;

        CoverageStoreInfo store = new CoverageStoreInfo();

        String prefixedName = namedLink.getName();
        int i = prefixedName.indexOf(":");
        if (-1 == i) {
            store.setName(prefixedName);
        } else {
            String workspace = prefixedName.substring(0, i);
            String name = prefixedName.substring(i + 1);
            store.setName(name);
            store.setWorkspace(new WorkspaceInfo().name(workspace));
        }
        return store;
    }
}
