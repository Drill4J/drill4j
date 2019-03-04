package com.epam.drill

import kotlinx.serialization.KSerializer

abstract class Ax<T>{

   abstract val deserializer: KSerializer<T>

}