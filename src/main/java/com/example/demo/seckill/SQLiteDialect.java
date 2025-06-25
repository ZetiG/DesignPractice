package com.example.demo.seckill;

import java.sql.Types;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupportImpl;

/**
 * Description:
 *
 * @author Zeti
 * @date 2025/6/25 10:53
 */
public class SQLiteDialect extends Dialect {

    public SQLiteDialect() {
        super();
        // 注册常用类型
        registerColumnType(Types.BIT, "boolean");
        registerColumnType(Types.TINYINT, "tinyint");
        registerColumnType(Types.SMALLINT, "smallint");
        registerColumnType(Types.INTEGER, "integer");
        registerColumnType(Types.BIGINT, "bigint");
        registerColumnType(Types.FLOAT, "float");
        registerColumnType(Types.DOUBLE, "double");
        registerColumnType(Types.VARCHAR, "varchar($l)");
        registerColumnType(Types.VARBINARY, "blob");
        registerColumnType(Types.BOOLEAN, "boolean");
        registerColumnType(Types.DATE, "date");
        registerColumnType(Types.TIMESTAMP, "datetime");
    }

    // 支持 ALTER TABLE
    @Override
    public boolean hasAlterTable() {
        return true;
    }

    // 不会自动删除外键
    @Override
    public boolean dropConstraints() {
        return false;
    }

    // 给表加列时的语法
    @Override
    public String getAddColumnString() {
        return "add column";
    }

    // 自增主键支持
    @Override
    public IdentityColumnSupport getIdentityColumnSupport() {
        return new IdentityColumnSupportImpl();
    }

    // SQLite 不支持批量删除约束
    @Override
    public boolean supportsCascadeDelete() {
        return false;
    }


}
