/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rat.web;

import it.geosolutions.imageio.pam.PAMDataset;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.FieldDefn;
import it.geosolutions.imageio.pam.PAMDataset.PAMRasterBand.Row;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.web.wicket.GeoServerDataProvider;

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
        return new AbstractProperty<>(f.getName()) {
            @Override
            public Object getPropertyValue(Row item) {
                return item.getF().get(f.getIndex());
            }
        };
    }

    @Override
    protected List<Row> getItems() {
        return rows;
    }
}
