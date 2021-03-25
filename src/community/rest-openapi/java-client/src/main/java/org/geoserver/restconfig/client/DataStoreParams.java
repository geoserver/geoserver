package org.geoserver.restconfig.client;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.NonNull;
import org.geoserver.openapi.v1.model.ConnectionParameterEntry;
import org.geoserver.openapi.v1.model.ConnectionParameters;

public class DataStoreParams {

    protected Map<String, String> params = new HashMap<>();

    public static Optional<ConnectionParameterEntry> find(
            @NonNull String name, @NonNull ConnectionParameters responseParams) {
        List<ConnectionParameterEntry> entry =
                responseParams.getEntry() == null
                        ? Collections.emptyList()
                        : responseParams.getEntry();
        return entry.stream().filter(e -> name.equals(e.getAtKey())).findFirst();
    }

    public void add(@NonNull String key, @NonNull String value) {
        params.put(key, value);
    }

    public Map<String, String> asMap() {
        return new HashMap<>(params);
    }

    public @Override String toString() {
        return params.toString();
    }

    public static class Shapefile extends DataStoreParams {

        public Shapefile() {
            add("fstype", "shape");
            add("filetype", "shapefile");
        }

        public Shapefile uri(@NonNull String uri) {
            add("url", uri);
            return this;
        }

        public Shapefile useMemoryMaps(boolean cacheAndReuseMemoryMaps) {
            add("cache and reuse memory maps", String.valueOf(cacheAndReuseMemoryMaps));
            return this;
        }

        public Shapefile createSpatialIndex(boolean spatialIndex) {
            add("create spatial index", String.valueOf(spatialIndex));
            return this;
        }

        public Shapefile useMemoryMappedBuffer(boolean memoryMappedBuffer) {
            add("memory mapped buffer", String.valueOf(memoryMappedBuffer));
            return this;
        }

        public Shapefile charset(Charset charset) {
            if (charset == null) {
                params.remove("charset");
            } else {
                add("charset", charset.name());
            }
            return this;
        }
    }

    public static class Tabfile extends DataStoreParams {

        public Tabfile() {
            add("type", "OGR");
            add("DriverName", "MapInfo File");
            // default values
            add("min connections", "1");
            add("Prime DataSources", "false");
            add("Connection timeout", "20");
            add("max connections", "20");
            add("Evictor tests per run", "3");
            add("Max data source idle time", "300");
        }

        public Tabfile uri(@NonNull String uri) {
            add("DatasourceName", uri);
            return this;
        }
    }
}
