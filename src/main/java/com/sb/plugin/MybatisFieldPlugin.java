package com.sb.plugin;

import com.sb.plugin.handler.BaseFieldTypeHandler;
import com.sb.plugin.handler.DefaultFieldTypeHandler;
import com.sb.plugin.common.FieldInfo;
import com.sb.plugin.common.ResultTableInfo;
import com.sb.plugin.util.ResolveUtil;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.*;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Type;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.stream.Collectors;

@Intercepts({
//        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds
//        .class, ResultHandler.class}),
//        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
//        @Signature(type = ParameterHandler.class, method = "getParameterObject", args = {}),
//        @Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class})
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})
})
public class MybatisFieldPlugin implements Interceptor {

    private final static Map<String, Map<String, List<String>>> configMap = new HashMap<>();
    private final static Map<Type, BaseFieldTypeHandler> typeHandlerRegisters = new HashMap<>();
    private final static DefaultFieldTypeHandler defaultTypeHandler = new DefaultFieldTypeHandler();

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String name = invocation.getMethod().getName();
        if (name.equals("prepare")) {
            handlerParams(invocation);
            return invocation.proceed();
        }

        return handlerResult(invocation);
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        Enumeration<String> enumeration = (Enumeration<String>) properties.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = enumeration.nextElement();
            String val = properties.getProperty(key);
            if (key.equals("handler")) {
                String[] hds = val.replace(" ", "").split(",");
                for (String str : hds) {
                    BaseFieldTypeHandler handler = ResolveUtil.newInstance(str, BaseFieldTypeHandler.class);
                    typeHandlerRegisters.put(handler.getRawType(), handler);
                }
            } else {
                if (!key.contains(".")) {
                    throw new UnsupportedOperationException("Keyword does not contain [.]");
                }
                String[] arr = key.split("\\.");
                String dbStr = arr[0];
                String tableStr = arr[1];
                Set<String> columnSet = Arrays.stream(val.replace(" ", "").split(",")).collect(Collectors.toSet());
                columnSet.addAll(columnSet.stream().map(x -> ResolveUtil.str2camel(x)).collect(Collectors.toSet()));
                Map<String, List<String>> listMap = configMap.get(dbStr);
                if (listMap == null) {
                    Map<String, List<String>> map = new HashMap<>();
                    map.put(tableStr, new ArrayList<>(columnSet));
                    configMap.put(dbStr, map);
                } else {
                    listMap.put(tableStr, new ArrayList<>(columnSet));
                }
            }
        }

        System.out.println("properties init config::: " + configMap);
    }

    private List<Object> handlerResult(Invocation invocation) throws Exception {
        ResultSetHandler resultSetHandler = (ResultSetHandler) invocation.getTarget();
        Statement statement = (Statement) invocation.getArgs()[0];

        String dbName = ResolveUtil.resolveDBName(statement.getConnection());
        BoundSql boundSql = ResolveUtil.getFieldVal(resultSetHandler, "boundSql");
        // 先解析返回字段, 再执行, 否则resultset为空
        ResultTableInfo resultTableInfo = ResolveUtil.resolveTableInfo(statement, boundSql, dbName);

        List<Object> es = resultSetHandler.handleResultSets(statement);

        if (configMap == null || configMap.isEmpty() || !configMap.containsKey(dbName)) {
            return es;
        }

        Map<String, List<String>> dbTableConfig = configMap.get(dbName);
        if (dbTableConfig == null || dbTableConfig.isEmpty()) {
            return es;
        }

        Map<String, FieldInfo> filedInfoMap = resultTableInfo.getFiledInfoMap();
        for (Map.Entry<String, List<String>> configEntry : dbTableConfig.entrySet()) {
            for (Map.Entry<String, FieldInfo> dbEntry : filedInfoMap.entrySet()) {
                // 模糊匹配
                if (!dbEntry.getKey().contains(configEntry.getKey())) {
                    continue;
                }

                List<String> configFields = configEntry.getValue();
                FieldInfo dbFields = dbEntry.getValue();
                List<String> columnNames = dbFields.getColumnNames();
                List<String> columnLabels = dbFields.getColumnLabels();
                for (int i = 0; i < columnNames.size(); i++) {
                    String columnName = columnNames.get(i);
                    String columnLabel = columnLabels.get(i);
                    if (configFields.contains(columnName)) {
                        updateResult(es, columnName);
                    } else if (configFields.contains(columnLabel)) {
                        updateResult(es, columnLabel);
                    } else {
                        //System.out.println("no mapping~~~~");
                    }
                }
            }
        }

        return es;
    }

    private void handlerParams(Invocation invocation) throws Exception {
        Object[] args = invocation.getArgs();
        Connection connection = (Connection) args[0];
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();

        String dbName = ResolveUtil.resolveDBName(connection);
        if (configMap == null || configMap.isEmpty() || !configMap.containsKey(dbName)) {
            return;
        }

        BoundSql boundSql = statementHandler.getBoundSql();
        String tableNameBySql = ResolveUtil.resolveTableNameBySql(boundSql.getSql());

        List<String> columnNames = new ArrayList<>();
        Map<String, List<String>> dbTableConfig = configMap.get(dbName);
        for (Map.Entry<String, List<String>> configEntry : dbTableConfig.entrySet()) {
            // 模糊匹配
            if (tableNameBySql.contains(configEntry.getKey())) {
                columnNames.addAll(configEntry.getValue());
            }
        }

        if (columnNames.isEmpty()) {
            return;
        }

        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        for (ParameterMapping ps : parameterMappings) {
            if (ps.getMode() != ParameterMode.OUT) {
                String property = ps.getProperty();
                boolean match = columnNames.contains(property);
                if (!match) {
                    continue;
                }

                Object val = ResolveUtil.getValFromObjByName(boundSql.getParameterObject(), property);
                boundSql.setAdditionalParameter(property, dealRequest(property, val));
            }
        }
    }

    private void updateResult(List<Object> list, String columnName) throws NoSuchFieldException,
            IllegalAccessException {
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            if (o instanceof Map) {
                Map map = (Map) o;
                if (map.keySet().contains(columnName)) {
                    map.put(columnName, dealResponse(columnName, map.get(columnName)));
                }
            } else if (o instanceof String || ResolveUtil.isBaseType(o) || o instanceof Date || o instanceof Timestamp) {
                list.set(i, dealResponse(columnName, o));
            } else {
                String camel = ResolveUtil.str2camel(columnName);
                Object fieldVal = ResolveUtil.getFieldVal(o, camel);
                ResolveUtil.setFieldVal(o, camel, dealResponse(columnName, fieldVal));
            }
        }
    }

    private Object dealResponse(String columnName, Object o) {
        if (o == null) {
            return null;
        }
        BaseFieldTypeHandler handler = typeHandlerRegisters.getOrDefault(ResolveUtil.getType(o), defaultTypeHandler);
        return handler.getResult(columnName, o);
    }

    private Object dealRequest(String columnName, Object o) {
        if (o == null) {
            return null;
        }
        BaseFieldTypeHandler handler = typeHandlerRegisters.getOrDefault(ResolveUtil.getType(o), defaultTypeHandler);
        return handler.setParam(columnName, o);
    }

}

