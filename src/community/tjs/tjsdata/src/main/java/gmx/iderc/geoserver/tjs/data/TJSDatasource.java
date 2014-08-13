/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.data;

import org.opengis.filter.Filter;

import javax.sql.RowSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * @author root
 */
public interface TJSDatasource {
    RowSet getRowSet() throws SQLException;

    RowSet getRowSet(Filter filter) throws SQLException;

    String[] getFields();

    ResultSetMetaData getResultSetMetaData() throws SQLException;
}
