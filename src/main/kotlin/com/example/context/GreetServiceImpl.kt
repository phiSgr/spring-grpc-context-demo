package com.example.context

import com.example.GreetServiceGrpcKt
import com.example.GreetingRequest
import com.example.GreetingResponse
import kotlinx.coroutines.delay
import org.lognet.springboot.grpc.GRpcService

/**
 * Imagine that this lives in another microservice.
 */
@GRpcService
class GreetServiceImpl : GreetServiceGrpcKt.GreetServiceCoroutineImplBase() {
    override suspend fun greet(request: GreetingRequest): GreetingResponse {
        delay(1000)
        return GreetingResponse.newBuilder()
            .setMessage("Hello ${request.name}! Your header is ${combinedHeaderContextKey.get()}").build()
    }
}
