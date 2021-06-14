package com.hyh.list

interface IParamProvider<Param : Any> {
    suspend fun getParam(): Param
}

object EmptyParamProvider : IParamProvider<Unit> {
    override suspend fun getParam() = Unit
}