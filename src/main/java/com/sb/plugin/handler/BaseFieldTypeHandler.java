package com.sb.plugin.handler;

import org.apache.ibatis.type.TypeReference;

/**
 * Created by zhaoqing on 2021-06-21 15:26
 */
public abstract class BaseFieldTypeHandler<T> extends TypeReference<T> {

    public abstract T setParam(String fieldName, T param);

    public abstract T getResult(String fieldName, T columnVal);

}
