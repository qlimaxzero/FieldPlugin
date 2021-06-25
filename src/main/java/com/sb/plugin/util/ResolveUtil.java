package com.sb.plugin.util;

import com.sb.plugin.common.FieldInfo;
import com.sb.plugin.common.ResultTableInfo;
import org.apache.ibatis.mapping.BoundSql;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by zhaoqing on 2021-06-25 11:05
 */
public class ResolveUtil {

    public static String resolveDBName(Connection connection) throws SQLException {
        String dbName = connection.getCatalog();
        if (StringUtils.isEmpty(dbName)) {
            DatabaseMetaData metaData = connection.getMetaData();
            String simpleUrl = metaData.getURL().split("\\?")[0];
            dbName = simpleUrl.substring(simpleUrl.lastIndexOf("/") + 1);
        }
        return dbName;
    }

    /**
     * 解析大概的表名
     *
     * @param sql
     * @return
     */
    public static String resolveTableNameBySql(String sql) {
        sql = sql.replaceAll("\t|\n", "").trim().toLowerCase();

        int from, where;
        if (sql.startsWith("select")) {
            from = sql.indexOf("from") + 4;
            where = sql.lastIndexOf("where");
        } else if (sql.startsWith("delete")) {
            from = sql.indexOf("from") + 4;
            where = sql.indexOf("where");
        } else if (sql.startsWith("update")) {
            from = sql.indexOf("update") + 6;
            where = sql.indexOf("set");
        } else if (sql.startsWith("insert")) {
            from = sql.indexOf("into") + 4;
            where = sql.indexOf("(");
        } else {
            throw new UnsupportedOperationException("不支持的SQL:" + sql);
        }

        if (where == -1) {
            return sql.substring(from);
        }
        return sql.substring(from, where).trim();
    }

    /**
     * @param statement
     * @param boundSql
     * @param dbName
     * @return
     * @throws SQLException
     */
    public static ResultTableInfo resolveTableInfo(Statement statement, BoundSql boundSql, String dbName) throws SQLException {
        ResultTableInfo resultInfo = new ResultTableInfo();

        resultInfo.setDbName(dbName);

        ResultSet resultSet = statement.getResultSet();
        if (resultSet != null) {
            ResultSetMetaData metaData = resultSet.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String tableName = metaData.getTableName(i);
                String columnName = metaData.getColumnName(i);
                String columnLabel = metaData.getColumnLabel(i);

                Map<String, FieldInfo> filedInfoMap = resultInfo.getFiledInfoMap();
                FieldInfo tempField = filedInfoMap.get(tableName);
                if (tempField == null) {
                    tempField = new FieldInfo();
                    tempField.setColumnNames(ResolveUtil.singletonList(columnName));
                    tempField.setColumnLabels(ResolveUtil.singletonList(columnLabel));
                    filedInfoMap.put(tableName, tempField);
                } else {
                    tempField.getColumnNames().add(columnName);
                    tempField.getColumnLabels().add(columnLabel);
                }
            }
        }

        // TODO 表名兜底, 存在空串则获取不到, 获取不到时根据SQL截取, 待优化
        Map<String, FieldInfo> filedMap = resultInfo.getFiledInfoMap();
        if (!filedMap.isEmpty() && filedMap.containsKey("")) {
            // 表名为空
            FieldInfo remove = filedMap.remove("");
            String tableNameBySql = ResolveUtil.resolveTableNameBySql(boundSql.getSql());
            filedMap.put(tableNameBySql, remove);
        }

        return resultInfo;
    }

    public static Type getType(Object o) {
        if (o == null) {
            return null;
        }
        ResolvableType resolvableType = ResolvableType.forInstance(o);
        return resolvableType.getType();
    }

    public static String str2camel(String columnName) {
        char sp = '_';
        char[] chars = columnName.toCharArray();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == sp) {
                if (++i < chars.length) {
                    sb.append(Character.toUpperCase(chars[i]));
                }
            } else {
                sb.append(chars[i]);
            }
        }
        return sb.toString();
    }

    public static <T> T newInstance(String classes, Class<T> tClass) {
        try {
            Class<?> clz = Class.forName(classes);
            return tClass.cast(clz.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Object getValFromObjByName(Object o, String fieldName) throws NoSuchFieldException,
            IllegalAccessException {
        if (o instanceof Map) {
            Map<String, Object> map = (Map) o;
            return map.get(fieldName);
        } else if (o instanceof String || ResolveUtil.isBaseType(o) || o instanceof Date || o instanceof Timestamp) {
            return o;
        } else {
            return ResolveUtil.getFieldVal(o, fieldName);
        }
    }

    public static <T> T getFieldVal(Object target, String fieldName) throws IllegalAccessException,
            NoSuchFieldException {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (NoSuchFieldException e) {
            throw e;
        } catch (IllegalAccessException e) {
            throw e;
        }
    }

    public static void setFieldVal(Object target, String fieldName, Object val) throws IllegalAccessException,
            NoSuchFieldException {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, val);
        } catch (NoSuchFieldException e) {
            throw e;
        } catch (IllegalAccessException e) {
            throw e;
        }
    }

    public static boolean isBaseType(Object o) {
        try {
            if (((Class) o.getClass().getField("TYPE").get(null)).isPrimitive()) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static List<String> singletonList(String columnName) {
        List<String> list = new ArrayList<>();
        list.add(columnName);
        return list;
    }

}
