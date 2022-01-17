package ru.spbstu

import kotlinx.benchmark.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Lazy1<T>: kotlin.Lazy<T> {
    private class Uninit<T>(val function: () -> T)

    var data: Any?

    constructor(uninit: () -> T) {
        data = Uninit(uninit)
    }

    override val value: T
        get(): T {
            when (val bound = data) {
                is Uninit<*> -> data = bound.function()
            }
            return data as T
        }

    override fun isInitialized(): Boolean = data !is Uninit<*>
}

fun <T> lazy1(body: () -> T) = Lazy1(body)

val LAZY2_UNINIT = Any()
inline fun <T> lazy2(crossinline body: () -> T) = object : kotlin.Lazy<T> {
    var data: Any? = LAZY2_UNINIT

    override val value: T
        get(): T {
            when {
                data === LAZY2_UNINIT -> data = body()
            }
            return data as T
        }

    override fun isInitialized(): Boolean = data !== LAZY2_UNINIT
}

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.NANOSECONDS)
open class Bench {
    @Benchmark
    fun defaultAccess(bh: Blackhole) {
        val lazy by lazy(LazyThreadSafetyMode.NONE) { "HEllo, ${listOf(1)}" }
        for (i in 0..100) {
            bh.consume(lazy)
        }
    }

    @Benchmark
    fun impl1Access(bh: Blackhole) {
        val lazy by lazy1 { "HEllo, ${listOf(1)}" }
        for (i in 0..100) {
            bh.consume(lazy)
        }
    }

    @Benchmark
    fun impl2Access(bh: Blackhole) {
        val lazy by lazy2 { "HEllo, ${listOf(1)}" }
        for (i in 0..100) {
            bh.consume(lazy)
        }
    }

    @Benchmark
    fun inlinedAccess(bh: Blackhole) {
        var lazy: Any? = LAZY2_UNINIT
        for (i in 0..100) {
            if (lazy === LAZY2_UNINIT) {
                lazy = "HEllo, ${listOf(1)}"
            }
            bh.consume(lazy)
        }
    }

    @Benchmark
    fun defaultCreate(bh: Blackhole) {
        for (i in 0..100) {
            bh.consume(lazy(LazyThreadSafetyMode.NONE) { "HEllo, ${listOf(1)}" })
        }
    }

    @Benchmark
    fun impl1Create(bh: Blackhole) {
        for (i in 0..100) {
            bh.consume(lazy1 { "HEllo, ${listOf(1)}" })
        }
    }

    @Benchmark
    fun impl2Create(bh: Blackhole) {
        for (i in 0..100) {
            bh.consume(lazy2 { "HEllo, ${listOf(1)}" })
        }
    }
}