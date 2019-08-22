package com.epam.drill

import platform.posix.*
import kotlinx.cinterop.*


fun doMkdir(path:String){
    mkdir(path, "0777".toInt(8).convert())
}