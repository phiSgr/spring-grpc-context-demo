package com.example.context

import com.example.GreetServiceGrpcKt
import com.example.GreetingRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class GreetingController(@Autowired val service: GreetServiceGrpcKt.GreetServiceCoroutineStub) {
    /**
     * The value of [combinedHeaderMetadataKey] is implicitly passed.
     * See [CombinedHeaderClientInterceptor]
     */
    @GetMapping("/hello")
    suspend fun hello(): Greeting {
        val res = service.greet(GreetingRequest.newBuilder().setName("Spring").build())
        return Greeting(res.message)
    }
}
