/*
 *  Copyright 2017 EPAM Systems <Igor_Kuzminykh@epam.com, Sergey_Larin@epam.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.epam.drill.plugin.api.processing

import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author Igor Kuzminykh
 */
object DrillConstants {
    val DRILL_HOME = File("stuff", ".drill")

    fun welcomeAdv() {
        Logger.getLogger(DrillConstants::class.java.name).log(Level.INFO,
                "       _            _            _          _             _     \n"
                        + "      /\\ \\         /\\ \\         /\\ \\       _\\ \\          _\\ \\   \n"
                        + "     /  \\ \\____   /  \\ \\        \\ \\ \\     /\\__ \\        /\\__ \\  \n"
                        + "    / /\\ \\_____\\ / /\\ \\ \\       /\\ \\_\\   / /_ \\_\\      / /_ \\_\\ \n"
                        + "   / / /\\/___  // / /\\ \\_\\     / /\\/_/  / / /\\/_/     / / /\\/_/ \n"
                        + "  / / /   / / // / /_/ / /    / / /    / / /         / / /      \n"
                        + " / / /   / / // / /__\\/ /    / / /    / / /         / / /       \n"
                        + "/ / /   / / // / /_____/    / / /    / / / ____    / / / ____   \n"
                        + "\\ \\ \\__/ / // / /\\ \\ \\  ___/ / /__  / /_/_/ ___/\\ / /_/_/ ___/\\ \n"
                        + " \\ \\___\\/ // / /  \\ \\ \\/\\__\\/_/___\\/_______/\\__\\//_______/\\__\\/ \n"
                        + "  \\/_____/ \\/_/    \\_\\/\\/_________/\\_______\\/    \\_______\\/      "
                        + "\n")
    }
}
