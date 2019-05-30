package com.epam.drill.dataclasses

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "JSON_MESSAGES")
data class JsonMessage(
    @Id @Column(name = "MESSAGE_ID")
    val id: String? = null,
    @Column
    val message: String? = null
)