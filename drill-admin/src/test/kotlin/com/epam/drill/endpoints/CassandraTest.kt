package com.epam.drill.endpoints

import com.epam.drill.dataclasses.AgentBuildVersion
import com.epam.drill.plugins.coverage.dataclasses.RawClassData
import com.epam.drill.plugins.coverage.dataclasses.RawScope
import com.epam.drill.plugins.coverage.dataclasses.RawTest
import com.epam.drill.plugins.coverage.dataclasses.TestType
import com.impetus.client.cassandra.common.CassandraConstants
import org.junit.*
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence
import javax.persistence.criteria.CriteriaQuery

class CassandraTest {
    companion object {

        private val PU = "cassandra_pu"

        private var emf: EntityManagerFactory? = null

        @BeforeClass
        @JvmStatic
        @Throws(Exception::class)
        fun SetUpBeforeClass() {
            val props = HashMap<String, String>()
            props["kundera.keyspace"] = "Agent1"
            props[CassandraConstants.CQL_VERSION] = CassandraConstants.CQL_VERSION_3_0
            emf = Persistence.createEntityManagerFactory(PU, props)
        }

        @AfterClass
        @JvmStatic
        @Throws(Exception::class)
        fun tearDownAfterClass() {
            if (emf != null) {
                emf!!.close()
                emf = null
            }
        }
    }

    private var em: EntityManager? = null
    @Before
    @Throws(Exception::class)
    fun setUp() {
        em = emf!!.createEntityManager()
    }

    @Test
    fun cassandraCRUD() {
        /*val elements = Scope(
            id = 666,
            name = "scopeName"
        )
        var build = Build(
            id = 101,
            buildName = "build",
            scopes = arrayListOf(elements)
        )

        em!!.persist(build)



        build = em!!.find(Build::class.java, "101")
        build.scopes?.forEach { println(it) }
        em!!.close()*/
//        Assert.assertNotNull(person)
//        Assert.assertEquals("101", person.personId)
//        Assert.assertEquals("dev", person.personName)
    }

   /* @Test
    fun cassandraCast() {
        val buildVersion = AgentBuildVersion("bbb", "aaa")
        em!!.persist(buildVersion)
        val q = em!!.createQuery("Select a from AgentBuildVersion a")
        val results = q.getResultList()
        val versions = results as List<AgentBuildVersion>
        println(versions.stringify())
        Assert.assertEquals(listOf(buildVersion), versions)
    }

    @Test
    fun anyListConversion(){
        //create raw object
        val rawData = RawClassData(3452345, "awesomeClass", listOf(true, false))
        val rawTest = RawTest(3425,"awesomeTest", TestType.AUTO.type, listOf(rawData))
        val rawScope = RawScope(3453,"awesomeScope", "3.2.1", listOf(rawTest))
        val scope: Scope = convert(rawScope)
        em!!.persist(scope)


        val results = em!!.createQuery("Select s from Scope s").resultList
        val scopes = results as List<Scope>
        println(scopes.stringify())
    }*/
}

