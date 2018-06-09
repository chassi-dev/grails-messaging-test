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
class ProducerConsumerRouteBuilder extends RouteBuilder {
    
    static Integer MAX_PRODUCER_BATCH_SIZE = 100
    static Integer producerBatchSize = 0
    static Integer maxConcurrentConsumers = 20
    
    @Autowired
    ProducerTemplate producerTemplate
    
    @Autowired
    ProducerConsumerService producerConsumerService
    
    void configure() {
        
        def enableProducerConsumerTest = "${System.getenv('ENABLE_CONSUMER_PRODCER_TEST')?.trim() ?: 'true'}"?.toString()?.toLowerCase()
        
        if (enableProducerConsumerTest == 'true') {
            
        
            // initial startup
            from("timer:wakeup-message?delay=500&repeatCount=1")
                .routeId('wakeup-message')
                .log('Starting producers')
                .process { Exchange exchange ->
                        producerBatchSize = 50
                    }
            
            // ramp down after 10 mins, iterating 30 times
            from("timer:ramping-down?delay=30s&period=1000&repeatCount=30")
                .routeId('ramping-down')
                .process { Exchange exchange ->
                        producerBatchSize = 0
                        exchange.in.body = producerConsumerService.getRemainingUnresolvedCount()
                    }
                .to('log:ramping-down')
            
            from("timer:producer-timer?delay=5000&period=500&fixedRate=true")
                .routeId('producer-timer')
                //.to('log:producer-timer')
                .setHeader('batchRecipientList', constant(''))
                .process { Exchange exchange ->
                        String targetRoute = 'direct:producer-create-instance'
                        String batchRecipientList = ''
                        if (producerBatchSize == 1) {
                            batchRecipientList = targetRoute
                        } else if (producerBatchSize > 1) {
                            Integer tmpBatchSize = producerBatchSize < MAX_PRODUCER_BATCH_SIZE ? producerBatchSize : MAX_PRODUCER_BATCH_SIZE
                            // as comma separated list
                            //batchRecipientList = ( [1..tmpBatchSize].collect { it.collect { targetRoute }.join(',') }[0] )
                            //println batchRecipientList
                            
                            // as List<String>
                            batchRecipientList = [targetRoute].multiply(tmpBatchSize)
                        }
                        
                        //exchange.in.headers['batchRecipientList'] = batchRecipientList
                        exchange.in.body = batchRecipientList
                    }
                //.to("log:producer-timer?showHeaders=true")
                
                // as comma separated list above
                //.recipientList( header('batchRecipientList'), ',' ).parallelProcessing()
                
                // as List<String> above
    //            .split(simple("header.batchRecipientList")).parallelProcessing().streaming()
    //                .to('direct:producer-create-instance')
    //            .end()
                
                // as List<String> above
                .split(body()).parallelProcessing().streaming()
                    .to('direct:producer-create-instance')
                .end()
    
            from('direct:producer-create-instance')
                .routeId('producer-create-instance')
                .process { Exchange exchange ->
                        exchange.in.setBody(producerConsumerService.createInstance(), LogMessage.class)
                    }
                .to("log:producer-create-instance")
                .to('direct:producer-queue')
            
            String producerQueueName = "producerconsumer.1.producer-queue"
            
            from('direct:producer-queue')
                .routeId('producer-queue')
                .marshal().json(JsonLibrary.Jackson, LogMessage.class, true)
                .to("activemq:queue:${producerQueueName}")
            
            from("activemq:queue:${producerQueueName}?maxConcurrentConsumers=${maxConcurrentConsumers}")
                .routeId('consumer-from-queue')
                .unmarshal().json(JsonLibrary.Jackson, LogMessage.class)
                .to("log:consumer-from-queue")
                .process { Exchange exchange ->
                        LogMessage logMessage = exchange.in.getBody(LogMessage)
                        exchange.in.headers['resolveInstanceResult'] = producerConsumerService.resolveInstance(logMessage?.logMessageId)
                    }
                //.delay(100)
        }

    }
}
