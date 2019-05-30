import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.OneToMany
import javax.persistence.Table

@Entity
@Table(name = "BUILD")
data class Build(
    @Id @Column(name = "BUILD_VERSION")
    var id: Long? = null,
    @Column(name = "BUILD_NAME")
    var buildName: String? = null,
    @OneToMany(cascade = [CascadeType.PERSIST])
    var scopes: List<Scope>? = null
)