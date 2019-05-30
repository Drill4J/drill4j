package com.epam.drill.storage

import io.ktor.application.Application
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import java.util.HashMap
import javax.persistence.EntityManager
import javax.persistence.Persistence
import org.kodein.di.generic.instance

class CassandraConnector(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    private val ems: HashMap<String, EntityManager> = hashMapOf()

    fun addEntityManager(keyspace: String): EntityManager {
        var res = ems[keyspace]
        if (res == null) {
            val params = HashMap<String, String>()
            params["kundera.keyspace"] = keyspace
            println(keyspace)

            val em = Persistence.createEntityManagerFactory("cassandra_pu", params).createEntityManager()
            ems[keyspace] = em
            res = em
        }
        return res!!
    }

    fun getEntityManagerByKeyspace(keyspace: String): EntityManager {
        return ems[keyspace]!!
    }

}


