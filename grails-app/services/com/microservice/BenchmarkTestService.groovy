package com.microservice

import groovy.time.TimeCategory
import groovy.time.TimeDuration
import org.apache.camel.ProducerTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Service
class BenchmarkTestService {
    
    @Autowired
    ProducerTemplate producerTemplate  // camel producer template
    
    Integer messageCount = 0
    
    Integer MAX_CONCURRENT_BENCH_THREADS = 100
    Integer concurrentThreadsEnv = "${System.getenv('BENCHMARK_THREADS')?.trim() ?: '10'}"?.toInteger()
    Integer concurrentThreads = concurrentThreadsEnv < MAX_CONCURRENT_BENCH_THREADS ? concurrentThreadsEnv : MAX_CONCURRENT_BENCH_THREADS
    
    Integer totalMessagesToSend = "${System.getenv('BENCHMARK_TOTAL_MSGS')?.trim() ?: '100'}"?.toInteger() ?: 10
    
    def runBenchmark() {
        
        def jsonDoc = ''
        
        // integer division is ok
        Integer msgCountPerThread = (Integer) totalMessagesToSend.intdiv(concurrentThreads)
        
        def myClosure = { num ->
            msgCountPerThread.times {
                String result = producerTemplate.requestBody('direct:benchmark-producer', jsonDoc.toString(), String.class)
            }
        }
        
        def threadPool = Executors.newFixedThreadPool(4)
        
        Date startTime = new Date()
        
        try {
            List<Future> futures = (1..concurrentThreads).collect { num ->
                threadPool.submit({ ->
                    myClosure num
                } as Callable);
            }
            
            // recommended to use following statement to ensure the execution of all tasks.
            futures.each {
                it.get()
                synchronized (messageCount) {
                    messageCount++
                }
            }
            
        } finally {
            threadPool.shutdown()
        }
        
        Date stopTime = new Date()
        TimeDuration timeDuration = TimeCategory.minus(stopTime, startTime)
        
        def messagesPerSecond = new Float(messageCount / timeDuration.toMilliseconds() * 1000.0).round(3)
        
        log.info "Total messages sent: ${totalMessagesToSend}"
        log.info "Time: ${timeDuration}"
        log.info "Messages per second: ${messagesPerSecond}"
    }
    
    def elapsedTime(Closure closure) {
        
        Date startTime = new Date()
        
            closure()
        
        Date stopTime = new Date()
        TimeDuration timeDuration = TimeCategory.minus(stopTime, startTime)
        
        return timeDuration
    }
    
}
