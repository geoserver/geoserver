package org.geoserver.restconfig.api.v1.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.geoserver.openapi.model.catalog.KeywordInfo;
import org.geoserver.openapi.model.catalog.NamespaceInfo;
import org.geoserver.openapi.v1.model.DoubleArrayResponse;
import org.geoserver.openapi.v1.model.NamedLink;
import org.geoserver.openapi.v1.model.ResourceResponseKeywords;
import org.geoserver.openapi.v1.model.StringArrayResponse;
import org.mapstruct.Mapping;

public interface ResourceResponseMapper {

    public default List<String> map(StringArrayResponse r) {
        return r == null || r.getString() == null ? null : new ArrayList<String>(r.getString());
    }

    public default List<KeywordInfo> map(ResourceResponseKeywords r) {
        return r == null || r.getString() == null
                ? null
                : r.getString().stream().map(this::mapKeyword).collect(Collectors.toList());
    }

    @Mapping(source = "name", target = "prefix")
    @Mapping(source = "href", target = "uri")
    public NamespaceInfo map(NamedLink l);

    public default KeywordInfo mapKeyword(String s) {
        if (s == null) return null;
        KeywordInfo ki = new KeywordInfo();
        if (-1 == s.indexOf(';')) {
            ki.value(s);
        } else {
            String spec = s.replace("\\", "").replace(";", "");
            String[] split = spec.split("@");
            ki.value(split[0]);
            for (int i = 1; i < split.length; i++) {
                String kv = split[i];
                String[] kvc = kv.split("=");
                if (kvc.length == 2) {
                    final String k = kvc[0];
                    String v = kvc[1];
                    if ("language".equals(k)) {
                        ki.language(v);
                    } else if ("vocabulary".equals(k)) {
                        ki.vocabulary(v);
                    }
                }
            }
        }
        return ki;
    }

    public default String mapCrs(java.lang.Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Map) {
            return (String) ((Map) value).get("$");
        }
        throw new IllegalStateException();
    }

    public default List<java.lang.Double> map(DoubleArrayResponse value) {
        if (value == null || value.getDouble() == null) return null;
        return new ArrayList<>(value.getDouble());
    }

    public default Double stringToDouble(String value) {
        if (value == null) return null;
        if ("inf".equals(value)) return Double.POSITIVE_INFINITY;
        if ("-inf".equals(value)) return Double.NEGATIVE_INFINITY;
        return Double.parseDouble(value);
    }
}
