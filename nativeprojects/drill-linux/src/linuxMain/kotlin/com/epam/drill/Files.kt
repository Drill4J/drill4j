package com.epam.drill

import platform.posix.mkdir
import kotlinx.cinterop.convert


fun doMkdir(path:String){
    mkdir(path, "0777".toInt(8).convert())
}