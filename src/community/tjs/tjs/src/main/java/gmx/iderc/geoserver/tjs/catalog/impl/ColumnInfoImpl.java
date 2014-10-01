/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package gmx.iderc.geoserver.tjs.catalog.impl;

import gmx.iderc.geoserver.tjs.catalog.ColumnInfo;

import java.io.Serializable;

/**
 * @author capote
 */
public class ColumnInfoImpl implements ColumnInfo, Serializable {

    String name;
    String type;
    String title;
    String abstractValue;
    int length;
    int decimals;
    String documentation;
    String valueUOM;
    Class sqlClassBinding;
    //esto de debe estar también, Alvaro Javier
    String purpose = "Attribute"; //valor por defecto

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setAbstract(String abstractValue) {
        this.abstractValue = abstractValue;
    }

    public String getAbstract() {
        return abstractValue;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setValueUOM(String valueUOM) {
        this.valueUOM = valueUOM;
    }

    public String getValueUOM() {
        return valueUOM;
    }

    public Class getSQLClassBinding() {
        if (sqlClassBinding == null) {
            sqlClassBinding = String.class;
        }
        return sqlClassBinding;
    }

    public void setSQLClassBinding(Class sqlClassBinding) {
        this.sqlClassBinding = sqlClassBinding;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }
}
