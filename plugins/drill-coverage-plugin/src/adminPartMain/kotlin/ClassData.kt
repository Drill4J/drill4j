package com.epam.drill.plugins.coverage

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "CLASS_DATA")
data class ClassData(
    @Id @Column(name = "DATA_ID")
    val id: Long? = null,
    @Column(name = "NAME")
    val className: String? = null,
    @Column(name = "PROBES")
    val probes: List<Boolean>? = null
)