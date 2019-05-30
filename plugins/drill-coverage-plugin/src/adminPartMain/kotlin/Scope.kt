package com.epam.drill.plugins.coverage

import javax.persistence.*

@Entity
@Table(name = "SCOPE")
class Scope(
    @Id @Column(name = "SCOPE_ID")
    val id: String? = null,
    @Column(name = "NAME")
    val name: String? = null,
    @Column
    val buildVersion: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], fetch=FetchType.EAGER)
    @JoinColumn(name="SCOPE_ID")
    val tests: List<Test>? = null
)