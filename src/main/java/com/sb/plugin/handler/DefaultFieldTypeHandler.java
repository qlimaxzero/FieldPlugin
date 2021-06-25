package com.sb.plugin.handler;

import com.sb.plugin.util.ResolveUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by zhaoqing on 2021-06-21 15:29
 */
public class DefaultFieldTypeHandler extends BaseFieldTypeHandler<Object> {

    private static final Log log = LogFactory.getLog(DefaultFieldTypeHandler.class);
    @Override
    public Object setParam(String fieldName, Object par) {
        log.info(String.format(" [%s] type is [%s]", fieldName, ResolveUtil.getType(par)));
        return par;
    }

    @Override
    public Object getResult(String fieldName, Object val) {
        log.info(String.format(" [%s] type is [%s]", fieldName, ResolveUtil.getType(val)));
        return val;
    }

}
