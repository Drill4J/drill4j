package com.epam.drill.storage

import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import io.ktor.application.Application
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.generic.instance
import org.litote.kmongo.KMongo
import org.litote.kmongo.getCollection


open class MongoClient(override val kodein: Kodein) : KodeinAware {
    private val app: Application by instance()
    var client: MongoClient? = null

    init {
        val mongoHost = app.environment.config.propertyOrNull("mongo.host")
        val mongoPort = app.environment.config.property("mongo.port").getString().toInt()

        val bindIp = if (System.getenv("isDocker") != null) {
            println("docker ENV!")
            "drillmongodb"
        } else mongoHost?.getString() ?: "localhost"
        try {
            client = KMongo.createClient(bindIp, mongoPort)
        } catch (io: Exception) {
            client = KMongo.createClient(bindIp, mongoPort)
            client?.startSession()
        } finally {
        }
    }


    inline fun <reified T : Any> storage(agentId: String, destination: String): MongoCollection<T> {
        client ?: throw RuntimeException("mongo client isnt ready.")
        return client!!.getDatabase(agentId).getCollection<T>(destination)

    }
}
