package com.sb.plugin.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zhaoqing on 2021-06-17 16:44
 */
public class ResultTableInfo {

    private String dbName;
    private Map<String, FieldInfo> filedInfoMap;

    public ResultTableInfo() {
        filedInfoMap = new HashMap<>();
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public Map<String, FieldInfo> getFiledInfoMap() {
        return filedInfoMap;
    }

    public void setFiledInfoMap(Map<String, FieldInfo> filedInfoMap) {
        this.filedInfoMap = filedInfoMap;
    }
}
