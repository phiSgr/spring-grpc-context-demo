package com.example.context

import io.grpc.*
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall
import io.grpc.kotlin.GrpcContextElement
import kotlinx.coroutines.withContext
import org.lognet.springboot.grpc.GRpcGlobalInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.server.CoWebFilter
import org.springframework.web.server.CoWebFilterChain
import org.springframework.web.server.ServerWebExchange

val headerNames = listOf("header-a", "header-b", "header-c")
val combinedHeaderContextKey: Context.Key<String> = Context.key("combinedHeader")
val combinedHeaderMetadataKey: Metadata.Key<String> =
    Metadata.Key.of("combinedHeader", Metadata.ASCII_STRING_MARSHALLER)

/**
 * Since 6.1.0, CoWebFilter passes the coroutineContext to the handler function.
 * [GreetingController.hello] will then see the value of [combinedHeaderContextKey]
 */
@Component
class HeaderCombiner : CoWebFilter() {
    override suspend fun filter(exchange: ServerWebExchange, chain: CoWebFilterChain) {
        val headers = exchange.request.headers
        // https://stackoverflow.com/questions/77354287/how-to-propagate-headers-received-in-spring-webflux-server-with-kotlin-coroutine
        val value = headerNames.joinToString(separator = "; ") { headerName ->
            "$headerName=${headers.getFirst(headerName)}"
        }

        withContext(
            GrpcContextElement(
                Context.current().withValue(combinedHeaderContextKey, value)
            )
        ) {
            chain.filter(exchange)
        }
    }
}

object CombinedHeaderClientInterceptor : ClientInterceptor {
    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel,
    ): ClientCall<ReqT, RespT> {
        /**
         * The value is in a [ThreadLocal] [Context]
         * Coroutine callers can see it because of [GrpcContextElement.updateThreadContext]
         */
        val value: String? = combinedHeaderContextKey.get()
        val newCall = next.newCall(method, callOptions)

        return if (value == null) newCall
        else object : SimpleForwardingClientCall<ReqT, RespT>(newCall) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                headers.put(combinedHeaderMetadataKey, value)
                super.start(responseListener, headers)
            }
        }
    }
}


/**
 * Basically this does the reverse of [CombinedHeaderClientInterceptor]
 */
@GRpcGlobalInterceptor
class CombinedHeaderServerInterceptor : ServerInterceptor {
    override fun <ReqT, RespT> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>,
    ): ServerCall.Listener<ReqT> {
        val value = headers.get(combinedHeaderMetadataKey)
        return if (value == null) next.startCall(call, headers)
        else Contexts.interceptCall(Context.current().withValue(combinedHeaderContextKey, value), call, headers, next)
    }
}
