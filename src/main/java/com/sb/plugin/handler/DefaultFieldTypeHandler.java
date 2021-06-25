package com.sb.plugin.handler;

/**
 * Created by zhaoqing on 2021-06-21 15:29
 */
public class DefaultFieldTypeHandler extends BaseFieldTypeHandler<Object> {

    @Override
    public Object setParam(String fieldName, Object par) {
        //log.info("par {} type is {}", fieldName, ResolveUtil.getType(par));
        return par;
    }

    @Override
    public Object getResult(String fieldName, Object val) {
        //log.info("val {} type is {}", fieldName, ResolveUtil.getType(val));
        return val;
    }

}
