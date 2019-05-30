//import javax.persistence.Column
//import javax.persistence.Entity
//import javax.persistence.Id
//import javax.persistence.OneToMany
//import javax.persistence.Table
//
//
//
//
//
//
//@Entity
//@Table(name = "TEST")
//data class Test(
//    @Id
//    val id: Long,
//    @Column(name = "NAME")
//    val testName: String? = null,
//    @Column(name = "TYPE")
//    val testType: Int? = null,
//    @OneToMany
//    @Column(name = "DATA")
//    val data: Set<ClassData>
//)
//
//@Entity
//@Table(name = "CLASS_DATA")
//data class ClassData(
//    @Id
//    val id: Long,
//    @Column(name = "NAME")
//    val className: String,
//    @Column(name = "PROBES")
//    val probes: List<Boolean>
//)
//
////For field 'testType' of class 'Test'
//enum class TestType(val type: Int) {
//    AUTO(0),
//    MANUAL(1),
//    PERFORMANCE(2)
//}