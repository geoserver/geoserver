/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.catalog;

/**
 * @author capote
 */
public interface ColumnInfo {

    void setName(String name);

    String getName();

    void setType(String type);

    String getType();

    void setLength(int length);

    int getLength();

    void setDecimals(int decimals);

    int getDecimals();

    void setTitle(String title);

    String getTitle();

    void setAbstract(String abstractValue);

    String getAbstract();

    void setDocumentation(String documentation);

    String getDocumentation();

    void setValueUOM(String valueUOM);

    String getValueUOM();

    public void setPurpose(String purpose);

    public String getPurpose();

    Class getSQLClassBinding();

    void setSQLClassBinding(Class sqlClassBinding);

}
