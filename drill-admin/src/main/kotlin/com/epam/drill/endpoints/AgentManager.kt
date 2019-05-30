package com.epam.drill.endpoints

import com.epam.drill.common.AgentInfo
import com.epam.drill.common.Family
import com.epam.drill.common.PluginBean
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware

class AgentManager(override val kodein: Kodein) : KodeinAware {
    fun agentConfiguration(agentId: String): AgentInfo {
        if (agentId == "Petclinic")
            return AgentInfo(
                id = agentId,
                name = "Petclinic App",
                groupName = "Dev",
                description = "This agent is configured only for developers",
                ipAddress = "",
                buildVersion = "fixed version",
                isEnable = true,
                additionalInfo = null,
                adminUrl = "",
                rawPluginNames = mutableSetOf(
                    PluginBean(
                        "coverage",
                        "awesomeCoverage",
                        "ohh the best of the best",
                        "custom",
                        Family.INSTRUMENTATION,
                        true,
                        "{\"pathPrefixes\": [\"org/drilspringframework/samples/petclinic\",\"com/epam/ta/reportportal\"], \"message\": \"hello from default plugin config... This is 'plugin_config.json file\"}"
                    )
                )
            )
        else  if (agentId == "IntegrationTests"){
            return AgentInfo(
                id = agentId,
                name = "Petclinic App",
                groupName = "Dev",
                description = "This agent is configured only for developers",
                ipAddress = "",
                buildVersion = "fixed version",
                isEnable = true,
                additionalInfo = null,
                adminUrl = "",
                rawPluginNames = mutableSetOf(
                    PluginBean(
                        "coverage",
                        "awesomeCoverage",
                        "ohh the best of the best",
                        "custom",
                        Family.INSTRUMENTATION,
                        true,
                        "{\"pathPrefixes\": [\"com/\",\"org/\"], \"message\": \"hello from default plugin config... This is 'plugin_config.json file\"}"
                    )
                )
            )
        }
        else return AgentInfo(
            id = agentId,
            name = "???",
            groupName = "???",
            description = "???",
            ipAddress = "???",
            buildVersion = "???",
            isEnable = true,
            additionalInfo = null,
            adminUrl = "",
            rawPluginNames = mutableSetOf()
        )
    }


}
