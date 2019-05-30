import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "SCOPE")
class Scope(
    @Id
    var id: Long? = null,
    @Column(name = "NAME")
    var name: String? = null
//    @OneToMany
//    @Column(name = "TESTS")
//    val tests: Set<Test>
)