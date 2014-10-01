package gmx.iderc.geoserver.tjs.data.jdbc.hsql;

import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import gmx.iderc.geoserver.tjs.data.jdbc.JDBC_TJSDataStoreFactory;
import gmx.iderc.geoserver.tjs.data.xml.SQLToXSDMapper;
import net.opengis.tjs10.*;
import org.eclipse.emf.common.util.EList;

import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 30/07/13
 * Time: 11:58
 * To change this template use File | Settings | File Templates.
 */
public class GDAS_Importer_Thread extends Thread {

    GDASType gdasType;
    String tableName;

    boolean alldone = false;
    boolean newGdas;

    public GDAS_Importer_Thread(GDASType gdasType, String tableName, boolean newGdas) {
        this.gdasType = gdasType;
        this.tableName = tableName;
        this.newGdas = newGdas;
    }

    @Override
    public void run() {
        super.run();
        alldone = importGDAS(gdasType);
    }

    public String createHostGDASTable(GDASType gdasType) {
        ColumnsetType columnset = gdasType.getFramework().getDataset().getColumnset();
        StringBuilder sqlbuilder = new StringBuilder();
        sqlbuilder.append("CREATE TABLE ");
        sqlbuilder.append(tableName + "(");
        ColumnType2 fktype = (ColumnType2) columnset.getFrameworkKey().getColumn().get(0);
        sqlbuilder.append(fktype.getName() + " " + getSQLTypeAsString(fktype.getType().getLiteral()));

        for (int index = 0; index < columnset.getAttributes().getColumn().size(); index++) {
            ColumnType1 attribute = (ColumnType1) columnset.getAttributes().getColumn().get(index);
            if (attribute.getName().equals(fktype.getName())) {
                continue;
            }
            sqlbuilder.append(", ");
            sqlbuilder.append(attribute.getName() + " " + getSQLTypeAsString(attribute.getType().getLiteral()));
        }

        sqlbuilder.append(");");
        String sql = sqlbuilder.toString();
        try {
            Statement statement = HSQLDB_GDAS_Cache.getConnection().createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
        return tableName;
    }

    private PreparedStatement getInsertStatement(GDASType gdasType, String tableName) {
        ColumnsetType columnset = gdasType.getFramework().getDataset().getColumnset();
        StringBuilder sqlbuilder = new StringBuilder();
        StringBuilder values = new StringBuilder();

        sqlbuilder.append("INSERT INTO " + tableName + "(");
        values.append(" VALUES (");

        ColumnType2 fktype = (ColumnType2) columnset.getFrameworkKey().getColumn().get(0);
        sqlbuilder.append(fktype.getName());
        values.append(" ?,");

        boolean hasValue = false;
        for (int index = 0; index < columnset.getAttributes().getColumn().size(); index++) {
            ColumnType1 attribute = (ColumnType1) columnset.getAttributes().getColumn().get(index);
            if (fktype.getName().equals(attribute.getName())) {
                continue;
            }

            if (hasValue) {
                values.append(", ");
            }
            sqlbuilder.append(", ");
            sqlbuilder.append(attribute.getName());
            values.append("?");
            hasValue = true;
        }

        sqlbuilder.append(")");
        values.append(")");
        sqlbuilder.append(values.toString());
        String insertSQL = sqlbuilder.toString();
        try {
            PreparedStatement statement = HSQLDB_GDAS_Cache.getConnection().prepareStatement(insertSQL);
            return statement;
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    public boolean importGDAS(GDASType gdasType) {
        if (newGdas){
            tableName = createHostGDASTable(gdasType);
        }
        if (tableName != null) {
            EList rows = gdasType.getFramework().getDataset().getRowset().getRow();
            PreparedStatement preparedStatement = getInsertStatement(gdasType, tableName);

            ColumnsetType columnset = gdasType.getFramework().getDataset().getColumnset();
            ColumnType2 frameworkKeyColumn = (ColumnType2) columnset.getFrameworkKey().getColumn().get(0);

            try {
                for (int index = 0; index < rows.size(); index++) {
                    RowType1 row = (RowType1) rows.get(index);
                    String value = ((KType) row.getK().get(0)).getValue();

                    setValue(frameworkKeyColumn.getType().getLiteral(), preparedStatement,  value, 1);
//                    preparedStatement.setString(1, value);
                    int findex = 1;
                    for (int j = 0; j < row.getV().size(); j++) {
                        ColumnType1 columnType1 = (ColumnType1) columnset.getAttributes().getColumn().get(j);
                        if (columnType1.getName().equals(frameworkKeyColumn.getName())) {
                            continue;
                        }
                        findex++;
                        VType vType = (VType) row.getV().get(j);
                        if (vType.getValue() != null){
                            setValue(columnType1.getType().getLiteral(), preparedStatement,  vType.getValue(), findex);
                        }
//                        preparedStatement.setString(findex, vType.getValue());
                    }
                    preparedStatement.executeUpdate();
                }
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return true;
    }

    Map<String, Integer> inverseMap = null;

    private String getSQLTypeAsString(String xsdType) {
        if (inverseMap == null) {
            SQLToXSDMapper mapper = new SQLToXSDMapper();
            inverseMap = mapper.getInverseMap();
        }
        Integer sqltype = inverseMap.get(xsdType);
        switch (sqltype) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.BIGINT: {
                return "integer";
            }
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.VARCHAR: {
                return "varchar";
            }
            case Types.REAL:
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.DOUBLE:
            case Types.FLOAT: {
                return "double";
            }
            case Types.DATE:
            case Types.TIME:
            case Types.TIMESTAMP: {
                return "timestamp";
            }

        }
        return "varchar";
    }

    private void setValue(String xsdType, PreparedStatement preparedStatement, String value, int index) throws SQLException {
        if (inverseMap == null) {
            SQLToXSDMapper mapper = new SQLToXSDMapper();
            inverseMap = mapper.getInverseMap();
        }
        Integer sqltype = inverseMap.get(xsdType);
        switch (sqltype) {
            case Types.INTEGER:
            case Types.SMALLINT:
            case Types.BIGINT: {
                preparedStatement.setInt(index, Integer.parseInt(value));
                return;
            }
            case Types.NVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.VARCHAR: {
                preparedStatement.setString(index, value);
                return;
            }
            case Types.REAL:
            case Types.DECIMAL:
            case Types.NUMERIC:
            case Types.DOUBLE:
            case Types.FLOAT: {
                preparedStatement.setDouble(index, Double.parseDouble(value));
                return;
            }
            case Types.DATE: {
                preparedStatement.setDate(index, Date.valueOf(value));
                return;
            }
            case Types.TIME: {
                preparedStatement.setTime(index, Time.valueOf(value));
                return;
            }
            case Types.TIMESTAMP: {
                preparedStatement.setTimestamp(index, Timestamp.valueOf(value));
                return;
            }
        }
        preparedStatement.setString(index, value);
    }
}
