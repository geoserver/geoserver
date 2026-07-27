/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pmtiles.web.data;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

/** Tests for {@link PMTilesDataStoreEditPanel#rewriteLegacyKeys(Map)} */
public class RewriteLegacyKeysTest {

    private Map<String, Serializable> rewrite(Map<String, Serializable> params) {
        Map<String, Serializable> mutable = new LinkedHashMap<>(params);
        PMTilesDataStoreEditPanel.rewriteLegacyKeys(mutable);
        return mutable;
    }

    @Test
    public void legacyPrefixRewrittenToStorage() {
        Map<String, Serializable> params = rewrite(Map.of(
                "io.tileverse.rangereader.provider", "s3",
                "io.tileverse.rangereader.s3.region", "us-west-2"));

        assertEquals(Map.of("storage.provider", "s3", "storage.s3.region", "us-west-2"), params);
    }

    @Test
    public void canonicalAndUnrelatedKeysUntouched() {
        Map<String, Serializable> original = Map.of(
                "namespace", "http://example.com",
                "pmtiles", "file:/data/europe.pmtiles",
                "storage.provider", "http",
                "storage.http.timeout-millis", 5000);

        assertEquals(original, rewrite(original));
    }

    @Test
    public void mixedLegacyAndCanonicalKeys() {
        Map<String, Serializable> params = rewrite(Map.of(
                "pmtiles", "file:/data/europe.pmtiles",
                "io.tileverse.rangereader.provider", "azure",
                "storage.azure.endpoint", "https://acc.blob.core.windows.net"));

        assertEquals(
                Map.of(
                        "pmtiles", "file:/data/europe.pmtiles",
                        "storage.provider", "azure",
                        "storage.azure.endpoint", "https://acc.blob.core.windows.net"),
                params);
    }

    @Test
    public void renamedKeysFollowStorageConfigNormalization() {
        Map<String, Serializable> params = rewrite(Map.of("io.tileverse.rangereader.gcs.host", "http://localhost:88"));

        assertEquals(Map.of("storage.gcs.endpoint", "http://localhost:88"), params);
    }
}
