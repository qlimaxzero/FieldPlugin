package com.sb.plugin.common;

import java.util.List;

/**
 * Created by zhaoqing on 2021-06-17 17:01
 */
public class FieldInfo {

    private List<String> columnNames;
    private List<String> columnLabels;

    public List<String> getColumnNames() {
        return columnNames;
    }

    public void setColumnNames(List<String> columnNames) {
        this.columnNames = columnNames;
    }

    public List<String> getColumnLabels() {
        return columnLabels;
    }

    public void setColumnLabels(List<String> columnLabels) {
        this.columnLabels = columnLabels;
    }
}
