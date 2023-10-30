package com.example.context

import com.example.GreetServiceGrpcKt
import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
class DemoApplication {
    @Bean
    fun channel(@Value("\${grpc.port}") port: Int): Channel =
        ManagedChannelBuilder.forAddress("localhost", port)
            .usePlaintext()
            .intercept(CombinedHeaderClientInterceptor)
            .build()

    @Bean
    fun greetingService(channel: Channel): GreetServiceGrpcKt.GreetServiceCoroutineStub =
        GreetServiceGrpcKt.GreetServiceCoroutineStub(channel)
}

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
}
