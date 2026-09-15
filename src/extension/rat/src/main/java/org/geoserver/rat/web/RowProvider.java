/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rat.web;

import it.geosolutions.imageio.pam.PAMDataset;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldDefn;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.Row;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.wms.featureinfo.RasterAttributeTableTypes;

public class RowProvider extends GeoServerDataProvider<Row> {

    private final List<PAMRasterBand> bands;
    private List<Row> rows;
    private PAMRasterBand band;

    public RowProvider(PAMDataset dataset, int bandIdx) {
        this.bands = dataset.getPAMRasterBand();
        setBand(bandIdx);
    }

    public void setBand(int bandIdx) {
        this.band = bands.get(bandIdx);
        this.rows = band.getGdalRasterAttributeTable().getRow();
    }

    @Override
    protected List<Property<Row>> getProperties() {
        return band.getGdalRasterAttributeTable().getFieldDefn().stream()
                .map(f -> toProperty(f))
                .collect(Collectors.toList());
    }

    private Property<Row> toProperty(FieldDefn f) {
        int index = f.getIndex();
        return new AbstractProperty<>(f.getName()) {
            @Override
            public Object getPropertyValue(Row item) {
                return item.getF().get(index);
            }

            @Override
            public Comparator<Row> getComparator() {
                return comparator(f);
            }
        };
    }

    /**
     * Sorts a column by value and not as text, so a number column does not put 10 before 9. Cells that cannot be
     * converted, an empty one included, sort first.
     */
    @SuppressWarnings("unchecked")
    private static Comparator<Row> comparator(FieldDefn f) {
        // a table can hold tens of thousands of rows, so decode each cell once per sort and not
        // once per comparison. The map stays empty until the first comparison, as the table asks
        // every column for a comparator on every render only to see if the column can be sorted.
        IdentityHashMap<Row, Object> decoded = new IdentityHashMap<>();
        return Comparator.comparing(
                r -> (Comparable<Object>) decoded.computeIfAbsent(
                        r,
                        row -> RasterAttributeTableTypes.toValue(
                                f.getType(), row.getF().get(f.getIndex()))),
                Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    @Override
    protected List<Row> getItems() {
        return rows;
    }
}
