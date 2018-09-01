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
class ExampleServiceRouteBuilder extends RouteBuilder {

    @Autowired
    ProducerTemplate producerTemplate

    void configure() {

        rest('/example-resource')
            .post()
                .id('example-resource-create')
                .to('direct:opr-example-resource-create')

        from('activemq:queue:example-resource-create')
            .id('queueu-example-resource-create')
            .to('direct:opr-example-resource-create')

        from('direct:opr-example-resource-create')
            .bean('example-resource-svc', 'create')

            // fire off and forget event
            .wireTap('direct:event-example-resource-create')

        // event reactor
        from('direct:event-example-resource-create')
            // thread (seda)
            .process {
                    // marshall into event
                }
            .to('activemq:topic:example-svc.1.event-example-resource-create')


        // event listener
        from('activemq:topic:example-svc.1.event-example-resource-create')
            .routeId('event-listener-example-resource-create')
            .to('activemq:queue:example-svc.1.dispatcher-event-example-resource-create')


        from('direct:dispatcher-event-example-resource-create')
















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
                .to("activemq:queue:${producerQueueName}?disableTimeToLive=true")

//            from("activemq:queue:${producerQueueName}?maxConcurrentConsumers=${concurrentThreads}")
//                .routeId('benchmark-consumer')
//                .unmarshal().json(JsonLibrary.Jackson, String.class)
//                //.to("log:benchmark-consumer")

    }
}
