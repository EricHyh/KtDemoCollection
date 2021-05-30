package com.hyh.list

interface IParamProvider {

    suspend fun getParam(sourceToken: Any): Any

}