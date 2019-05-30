package com.epam.drill.endpoints

import Build
import Scope
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.EntityManagerFactory
import javax.persistence.Persistence

class CassandraTest {
    companion object {

        private val PU = "cassandra_pu"

        private var emf: EntityManagerFactory? = null

        @BeforeClass
        @JvmStatic
        @Throws(Exception::class)
        fun SetUpBeforeClass() {
            val props = HashMap<String, String>()
            props["kundera.keyspace"] = "Agent"
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
        val elements = Scope(
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
        em!!.close()
//        Assert.assertNotNull(person)
//        Assert.assertEquals("101", person.personId)
//        Assert.assertEquals("dev", person.personName)
    }
}

