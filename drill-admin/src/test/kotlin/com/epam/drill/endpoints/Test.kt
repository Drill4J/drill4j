package com.epam.drill.endpoints
import javax.persistence.*

@Entity
@Table(name = "TEST")
data class Test(
    @Id @Column(name = "TEST_ID")
    val id: String? = null,
    @Column(name = "NAME")
    val testName: String? = null,
    @Column(name = "TYPE")
    val testType: Int? = null,
    @OneToMany(cascade = [CascadeType.ALL], fetch=FetchType.EAGER)
    val data: List<ClassData>? = null
)