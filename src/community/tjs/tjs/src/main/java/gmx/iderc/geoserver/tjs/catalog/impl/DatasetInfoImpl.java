/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gmx.iderc.geoserver.tjs.catalog.impl;

import gmx.iderc.geoserver.tjs.catalog.*;
import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import gmx.iderc.geoserver.tjs.data.TJSDatasource;
import gmx.iderc.geoserver.tjs.data.xml.SQLToXSDMapper;
import org.geotools.util.NullProgressListener;

import java.io.Serializable;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author root
 */

// TODO: THijs: should this be serializable?

public class DatasetInfoImpl extends TJSCatalogObjectImpl implements DatasetInfo, Serializable {

    String frameworkId;
    String dataStoreId;

    transient DataStoreInfo dataStoreInfo;
    transient FrameworkInfo frameworkInfo;

    Date referenceDate;
    String version;
    String documentation;
    String organization;
    String datasetName;
    String geoKeyField;

    String defaultStyle;

    String datasetUri;

    HashMap<String, ColumnInfo> columns = new HashMap<String, ColumnInfo>();
    private boolean autojoin;

    /* for serializable */
    public DatasetInfoImpl() {
        // dummy implementation, to avoid serialization?
    }

    public DatasetInfoImpl(TJSCatalog catalog) {
        super(catalog);
    }

    public FrameworkInfo getFramework() {
        if (frameworkInfo != null) {
            return frameworkInfo;
        }
        frameworkInfo = getCatalog().getFramework(frameworkId);
        return frameworkInfo;
    }

    public void setFramework(FrameworkInfo framework) {
        this.frameworkInfo = framework;
        this.frameworkId = framework.getId();
        updateUri();
    }

    @Override
    public void loadDefault() {
        setId(TJSCatalogFactoryImpl.getIdForObject(this));
        setName("Default DatasetInfo");
        setDescription("Default Dataset for testing propose.");
    }

    //TODO: sobreescribir aqui no hace falta?
    //Alvaro Javier Fuentes Suarez, 11:30 p.m. 1/8/13
    @Override
    public void accept(TJSCatalogVisitor visitor) {
        visitor.visit((DatasetInfo) this);
    }

    public Date getRefererenceDate() {
        return referenceDate;
    }

    public void setRefererenceDate(Date date) {
        this.referenceDate = date;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String uri) {
        this.documentation = uri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public DataStoreInfo getDataStore() {
        if (dataStoreInfo != null) {
            return dataStoreInfo;
        }
        dataStoreInfo = getCatalog().getDataStore(dataStoreId);
        return dataStoreInfo;
    }

    public void setDataStore(DataStoreInfo dataStoreInfo) {
        this.dataStoreInfo = dataStoreInfo;
        this.dataStoreId = dataStoreInfo.getId();
    }

    void updateUri() {
        if (getName() != null) {
            if (getFramework() != null) {
                datasetUri = getFramework().getUri().concat("/").concat(getName());
            }
        } else {
            if (getFramework() != null) {
                datasetUri = getFramework().getUri().concat("/");
            }
        }
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        updateUri();
    }

    public String getDatasetUri() {
        if (datasetUri == null) {
            updateUri();
        }
        return datasetUri;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public Date getReferenceDate() {
        return this.referenceDate;
    }

    public void setReferenceDate(Date refDate) {
        this.referenceDate = refDate;
    }

    public String getGeoKeyField() {
        return geoKeyField;
    }

    public void setGeoKeyField(String geoKeyField) {
        this.geoKeyField = geoKeyField;
    }

    public TJSDatasource getTJSDatasource() {
        TJSDataStore store = getDataStore().getTJSDataStore(new NullProgressListener());
        return store.getDatasource(datasetName, getDataStore().getConnectionParameters());
    }

    private void updateColumns() throws SQLException {

        ResultSetMetaData rstMeta = getTJSDatasource().getResultSetMetaData();
        SQLToXSDMapper mapper = new SQLToXSDMapper();
        for (int i = 0; i < rstMeta.getColumnCount(); i++) {
            ColumnInfo column = new ColumnInfoImpl();
            column.setName(rstMeta.getColumnName(i + 1));
            int type = rstMeta.getColumnType(i + 1);
            column.setType(mapper.map(type));
            column.setSQLClassBinding(rstMeta.getClass());
            switch (type) {
                case Types.INTEGER:
                case Types.BIGINT:
                case Types.DECIMAL:
                case Types.DOUBLE:
                case Types.FLOAT:
                case Types.NUMERIC:
                case Types.REAL: {
                    column.setDecimals(rstMeta.getScale(i + 1));
                    column.setLength(rstMeta.getColumnDisplaySize(i + 1));
                    columns.put(column.getName(), column);
                    break;
                }
                case Types.VARCHAR:
                case Types.NVARCHAR:
                case Types.CHAR:
                case Types.LONGNVARCHAR: {
                    column.setDecimals(0);
                    column.setLength(rstMeta.getPrecision(i + 1));
                    columns.put(column.getName(), column);
                    break;
                }
            }
        }
    }

    public List<ColumnInfo> getColumns() {
        if (columns == null) {
            columns = new HashMap<String, ColumnInfo>();
        }
        if (columns.isEmpty()) {
            try {
                updateColumns();
            } catch (SQLException ex) {
                Logger.getLogger(DatasetInfoImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        List<ColumnInfo> retValues = new ArrayList<ColumnInfo>();
        retValues.addAll(columns.values());
        return retValues;
    }

    public ColumnInfo getColumn(String name) {
        if (columns == null) {
            getColumns();
        }
        return columns.get(name);
    }

    @Override
    public String getDefaultStyle() {
        return defaultStyle;
    }

    @Override
    public void setDefaultStyle(String styleName) {
        this.defaultStyle = styleName;
    }

    @Override
    public boolean getAutoJoin() {
        return autojoin;
    }

    @Override
    public void setAutoJoin(boolean autoJoin) {
        autojoin = autoJoin;
    }

}

