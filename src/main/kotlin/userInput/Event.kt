package com.code.gamerg8.nami.userInput

import kotlin.reflect.KFunction1

class Event<T> {
    private val handlers = arrayListOf<KFunction1<T, Unit>>()
    operator fun plusAssign(handler: KFunction1<T, Unit>) { handlers.add(handler) }
    operator fun minusAssign(handler: KFunction1<T, Unit>) { handlers.remove(handler)}
    operator fun invoke(value: T) { for (handler in handlers) handler(value) }
}

val e = Event<String>() // define event

fun main(args : Array<String>) {
    e += ::DoTheThing
    e("sdfsdf") // invoke
}

fun DoTheThing(text: String){
    println(text)
}