import javax.persistence.*

@Entity
@Table(name = "TEST")
data class Test(
    @Id @Column(name = "TEST_ID")
    val id: Long? = null,
    @Column(name = "NAME")
    val testName: String? = null,
    @Column(name = "TYPE")
    val testType: Int? = null,
    @OneToMany(cascade = [CascadeType.ALL], fetch=FetchType.EAGER)
    @JoinColumn(name="TEST_ID")
    val data: List<ClassData>? = null
)