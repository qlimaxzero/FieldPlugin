package com.test;

import com.sb.plugin.handler.BaseFieldTypeHandler;

/**
 * Created by zhaoqing on 2021-06-21 15:29
 */
public class EncryptStringFieldHandler extends BaseFieldTypeHandler<String> {

    @Override
    public String setParam(String name, String val) {
        if (val == null) {
            return val;
        }

        if (val.length() % 32 == 0) {
            return val + "::STRadadING";
        }

        return val;
    }

    @Override
    public String getResult(String name, String val) {
        if (val == null) {
            return val;
        }

        if (val.length() % 32 == 0) {
            return val + "::STRINadaG::RES";
        }

        return val;
    }

}
