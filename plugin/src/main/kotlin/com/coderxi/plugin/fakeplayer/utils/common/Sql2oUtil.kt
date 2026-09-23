package com.coderxi.plugin.fakeplayer.utils.common

import org.sql2o.Connection

object Sql2oUtil {

    fun checkTableExists(conn: Connection, tableName: String) = runCatching {
        conn.createQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name=:name")
            .addParameter("name", tableName)
            .executeScalar(Int::class.java) > 0
    }.getOrDefault(false)

}