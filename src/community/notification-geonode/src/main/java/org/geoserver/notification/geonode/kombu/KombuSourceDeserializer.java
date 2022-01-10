/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.geonode.kombu;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;

public class KombuSourceDeserializer extends StdDeserializer<KombuSource> {

    private static final long serialVersionUID = 6089865078148924995L;

    public KombuSourceDeserializer() {
        super(KombuSource.class);
    }

    @Override
    public KombuSource deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        KombuSource ret = null;
        JsonNode node = jp.getCodec().readTree(jp);
        String type = node.get("type").asText();
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        Class<? extends KombuSource> sourceClass = null;
        if (type.equals("FeatureTypeInfo")) {
            sourceClass = KombuFeatureTypeInfo.class;
        }
        if (type.equals("WorkspaceInfo")) {
            sourceClass = KombuWorkspaceInfo.class;
        }
        if (type.equals("NamespaceInfo")) {
            sourceClass = KombuNamespaceInfo.class;
        }
        if (type.equals("LayerInfo")) {
            sourceClass = KombuLayerInfo.class;
        }
        if (type.equals("WMSLayerInfo")) {
            sourceClass = KombuWMSLayerInfo.class;
        }
        if (type.equals("StoreInfo")) {
            sourceClass = KombuStoreInfo.class;
        }
        if (type.equals("CoverageInfo")) {
            sourceClass = KombuCoverageInfo.class;
        }
        if (type.equals("LayerGroupInfo")) {
            sourceClass = KombuLayerGroupInfo.class;
        }
        if (sourceClass != null) {
            ret = mapper.readValue(node.toString(), sourceClass);
        }
        return ret;
    }
}
