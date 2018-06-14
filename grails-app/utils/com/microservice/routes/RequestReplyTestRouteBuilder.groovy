package com.microservice.routes

import com.microservice.ProducerConsumerService
import com.microservice.dtos.LogMessage
import org.apache.camel.Exchange
import org.apache.camel.ProducerTemplate
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.dataformat.JsonLibrary
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RequestReplyTestRouteBuilder extends RouteBuilder {
    
    static Integer MAX_PRODUCER_BATCH_SIZE = 100
    static Integer producerBatchSize = 0
    static Integer maxConcurrentConsumers = 20
    
    @Autowired
    ProducerTemplate producerTemplate
    
    @Autowired
    ProducerConsumerService producerConsumerService
    
    String[] stringList = [
            'ABCDEF',
            '123456',
            'A1B2C3',
        ]
    
    void configure() {
        
        def enableRequestReplyTest = "${System.getenv('ENABLE_REQUEST_REPLY_TEST')?.trim() ?: 'true'}"?.toString()?.toLowerCase()
        
        if (enableRequestReplyTest == 'true') {
            
            // initial startup
            from("timer:wakeup-reqrplyfn?delay=500&repeatCount=5&period=1000")
                .routeId('wakeup-reqrplyfn')
                .process { Exchange exchange ->
                        exchange.in.setBody(stringList[new Random().nextInt(stringList.length)], String.class)
                    }
                .convertBodyTo(String.class)
                .to('direct:opr-reqrplyfn')
            
            String producerQueueName = "producerconsumer.1.reqrplyfn"
            
            from('direct:opr-reqrplyfn')
                .routeId('opr-reqrplyfn')
                .marshal().json(JsonLibrary.Jackson, String.class, true)
                .convertBodyTo(String.class)
                .to('log:opr-reqrplyfn-in')
                .to("activemq:queue:${producerQueueName}?exchangePattern=InOut")
                .to('log:opr-reqrplyfn-out')
            
            from("activemq:queue:${producerQueueName}?maxConcurrentConsumers=${maxConcurrentConsumers}")
                .routeId('queue-reqrplyfn')
                .unmarshal().json(JsonLibrary.Jackson, String.class)
                //.to("log:queue-reqrplyfn")
                .process { Exchange exchange ->
                        String message = exchange.in.getBody(String.class) ?: '(null)'
                        message = message.reverse()
                        exchange.in.setBody(message)
                    }
                //.delay(100)
        }

    }
}













