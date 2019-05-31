package com.epam.drill.endpoints

import javax.persistence.*

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