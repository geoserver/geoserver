package gmx.iderc.geoserver.tjs.data.gdas;

import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;
import gmx.iderc.geoserver.tjs.data.xml.ClassToXSDMapper;
import net.opengis.tjs10.ColumnType1;
import net.opengis.tjs10.GDASType;

import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 30/07/13
 * Time: 14:08
 * To change this template use File | Settings | File Templates.
 */
public class GDAS_ColumnInfo extends ReadonlyColumnInfo {

    transient GDASType gdasType;
    transient ColumnType1 columnType;

    public GDAS_ColumnInfo(ColumnType1 columnType) {
        this.columnType = columnType;
    }

    @Override
    public String getName() {
        return columnType.getName();  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getType() {
        return columnType.getType().getLiteral();
    }

    @Override
    public int getLength() {
        return columnType.getLength().intValue();
    }

    @Override
    public int getDecimals() {
        return columnType.getDecimals().intValue();
    }

    @Override
    public String getTitle() {
        return columnType.getTitle();
    }

    @Override
    public String getAbstract() {
        return columnType.getAbstract().toString();
    }

    @Override
    public String getDocumentation() {
        return columnType.getDocumentation();
    }

    @Override
    public String getValueUOM() {
        return "";
        //return columnType.getValues().;
    }

    @Override
    public String getPurpose() {
        return columnType.getPurpose().getLiteral();
    }

    @Override
    public Class getSQLClassBinding() {
        ClassToXSDMapper mapper = new ClassToXSDMapper();
        return mapper.getInverseMap().get(columnType.getType().getLiteral());
    }
}
