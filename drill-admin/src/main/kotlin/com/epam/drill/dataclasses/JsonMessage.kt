package com.epam.drill.dataclasses

import com.epam.drill.common.ConnectedTable.uniqueIndex
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IdTable


object JsonMessages : IdTable<String>() {
    override val id: org.jetbrains.exposed.sql.Column<EntityID<String>> =
        varchar("version", length = 100).primaryKey().entityId().uniqueIndex()
    val message = text("message")
}


class JsonMessage(id: EntityID<String>) : org.jetbrains.exposed.dao.Entity<String>(id) {
    companion object : EntityClass<String, JsonMessage>(JsonMessages)

    var message by JsonMessages.message
}
