package com.microservice.routes

import com.microservice.BenchmarkTestService
import com.microservice.ProducerConsumerService
import com.microservice.dtos.LogMessage
import org.apache.camel.Exchange
import org.apache.camel.ExchangePattern
import org.apache.camel.ProducerTemplate
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.dataformat.JsonLibrary
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BenchmarkTestRouteBuilder extends RouteBuilder {
    
    static Integer MAX_CONCURRENT_BENCH_THREADS = 100
    static Integer concurrentThreadsEnv = "${System.getenv('BENCHMARK_THREADS')?.trim() ?: '10'}"?.toInteger()
    static Integer concurrentThreads = concurrentThreadsEnv < MAX_CONCURRENT_BENCH_THREADS ? concurrentThreadsEnv : MAX_CONCURRENT_BENCH_THREADS
    
    @Autowired
    ProducerTemplate producerTemplate
    
    @Autowired
    BenchmarkTestService benchmarkTestService
    
    void configure() {
        
        def enableBenchmarkTest = "${System.getenv('ENABLE_BENCHMARK_TEST')?.trim() ?: 'true'}"?.toString()?.toLowerCase()
        
        if (enableBenchmarkTest == 'true') {
            
        
            // initial startup
            from("timer:benchmark-run?delay=1000&repeatCount=1")
                .routeId('benchmark-wake')
                .log('Starting benchmark')
                .process { Exchange exchange ->
                        benchmarkTestService.runBenchmark()
                    }
                .log('Benchmark complete')
            
            // periodic progress
            
            
            String producerQueueName = "messagingtest.1.benchmark"
            
            from('direct:benchmark-producer')
                .routeId('benchmark-producer')
                .setExchangePattern(ExchangePattern.InOnly)
                .marshal().json(JsonLibrary.Jackson, String.class, true)
                .to("activemq:topic:${producerQueueName}?disableTimeToLive=true")
            
//            from("activemq:queue:${producerQueueName}?maxConcurrentConsumers=${concurrentThreads}")
//                .routeId('benchmark-consumer')
//                .unmarshal().json(JsonLibrary.Jackson, String.class)
//                //.to("log:benchmark-consumer")
        }

    }
}
