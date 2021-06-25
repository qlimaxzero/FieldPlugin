# FieldPlugin

## 基于Mybatis的拦截器实现的字段拦截处理器

### 1. 配置mybatis-config.xml文件
```xml
<plugin interceptor="com.sb.demo.aspect.DataSourceMybatisPlugin">
  <!-- 配置过滤的库.表 字段, 逗号分隔 -->
  <property name="dbName.tableName" value="phone, bank_number"/>
  <property name="test.user" value="unit_phone, name"/>
  <!-- 配置处理器, 逗号分隔 -->
  <property name="handler" value="com.sb.demo.aspect.EncryptStringFieldHandler"/>
</plugin>
```

### 2. 实现基础处理器
```java
public class EncryptStringFieldHandler extends BaseFieldTypeHandler<String> {

    @Override
    public String setParam(String name, String val) {
        if (val == null) {
            return val;
        }
        return val + "::STRING";
    }

    @Override
    public String getResult(String name, String val) {
        if (val == null) {
            return val;
        }
        return val + "::STRING::RES";
    }

}
```
