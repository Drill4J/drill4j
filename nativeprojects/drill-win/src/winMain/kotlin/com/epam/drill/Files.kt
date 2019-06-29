package com.epam.drill

import platform.posix.mkdir

fun doMkdir(path:String){
    mkdir(path)
}