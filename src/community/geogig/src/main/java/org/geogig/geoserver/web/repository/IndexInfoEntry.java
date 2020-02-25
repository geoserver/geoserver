/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import static com.google.common.base.Objects.equal;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.locationtech.geogig.repository.IndexInfo;
import org.locationtech.geogig.repository.IndexInfo.IndexType;

/** A {@link IndexInfo} representation for the presentation layer */
public class IndexInfoEntry implements Serializable {

    private static final long serialVersionUID = 4290576065610816811L;

    private Integer id;

    private String layer;

    private String indexedAttribute;

    private IndexType indexType;

    private List<String> extraAttributes;

    public IndexInfoEntry() {
        this.layer = "";
        this.indexedAttribute = "";
        this.indexType = null;
        this.extraAttributes = Lists.newArrayList();
        this.id = null;
    }

    public IndexInfoEntry(
            String layer,
            String indexedAttribute,
            IndexType indexType,
            List<String> extraAttributes) {
        this.layer = layer;
        this.indexedAttribute = indexedAttribute;
        this.indexType = IndexType.QUADTREE;
        this.extraAttributes = extraAttributes;
        this.id = hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IndexInfoEntry)) {
            return false;
        }
        if (o == this) {
            return true;
        }
        IndexInfoEntry i = (IndexInfoEntry) o;
        return equal(layer, i.layer)
                && equal(indexedAttribute, i.indexedAttribute)
                && equal(indexType, i.indexType)
                && equal(extraAttributes, i.extraAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ConfigEntry.class, layer, indexedAttribute, indexType);
    }

    public String getLayer() {
        return layer;
    }

    public void setLayer(String layer) {
        this.layer = layer;
    }

    public String getIndexedAttribute() {
        return indexedAttribute;
    }

    public void setIndexedAttribute(String indexedAttribute) {
        this.indexedAttribute = indexedAttribute;
    }

    @Nullable
    Integer getId() {
        return id;
    }

    public static IndexInfoEntry fromIndexInfo(IndexInfo indexInfo) {
        String layer = indexInfo.getTreeName();
        String indexedAttribute = indexInfo.getAttributeName();
        IndexType indexType = indexInfo.getIndexType();
        List<String> extraAttributes =
                Lists.newArrayList(IndexInfo.getMaterializedAttributeNames(indexInfo));
        return new IndexInfoEntry(layer, indexedAttribute, indexType, extraAttributes);
    }

    public static ArrayList<IndexInfoEntry> fromIndexInfos(List<IndexInfo> indexInfos) {
        ArrayList<IndexInfoEntry> indexInfoEntries = new ArrayList<>();
        for (IndexInfo info : indexInfos) {
            indexInfoEntries.add(fromIndexInfo(info));
        }
        return indexInfoEntries;
    }
}
