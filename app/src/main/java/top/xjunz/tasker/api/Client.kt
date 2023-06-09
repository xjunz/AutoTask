/*
 * Copyright (c) 2023 xjunz. All rights reserved.
 */

package top.xjunz.tasker.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author xjunz 2023/03/01
 */
class Client {

    private val host = "http://no-such-host:886"

    private val httpClient: HttpClient = HttpClient(CIO) {
        expectSuccess = false
        engine {
            requestTimeout = 5000
        }
    }

    suspend fun getCurrentPrice(): HttpResponse {
        return withContext(Dispatchers.IO) {
            httpClient.post("$host/p/c")
        }
    }

    suspend fun createOrder(deviceId: String): HttpResponse {
        return withContext(Dispatchers.IO) {
            httpClient.post("$host/o/c") {
                encryptHeaders(deviceId)
            }
        }
    }

    suspend fun checkOderState(orderId: String): HttpResponse {
        return withContext(Dispatchers.IO) {
            httpClient.post("$host/o/r") {
                encryptHeaders(orderId)
            }
        }
    }

    suspend fun restorePurchase(uid: String, deviceId: String): HttpResponse {
        return withContext(Dispatchers.IO) {
            httpClient.post("$host/u/c") {
                encryptHeaders(uid, deviceId)
            }
        }
    }

    suspend fun consumeRedeem(code: String, deviceId: String): HttpResponse {
        return withContext(Dispatchers.IO) {
            httpClient.post("$host/g/c") {
                encryptHeaders(code, deviceId)
            }
        }
    }

    suspend fun checkForUpdates(): HttpResponse {
        return withContext(Dispatchers.IO) {
            httpClient.get("https://api.bq04.com/apps/latest/6404da310d81cc43daf6b431") {
                parameter("api_token", "5823a317109145ad2d9257d8c81cb641")
            }
        }
    }

    fun close() {
        httpClient.close()
    }
}