package com.microservice

import org.apache.activemq.camel.component.ActiveMQComponent
import org.apache.activemq.jms.pool.PooledConnectionFactory
import org.apache.activemq.spring.ActiveMQConnectionFactory
import org.apache.camel.CamelContext
import org.apache.camel.ConsumerTemplate
import org.apache.camel.ProducerTemplate
import org.apache.camel.component.jms.JmsConfiguration
import org.apache.camel.spring.CamelBeanPostProcessor
import org.apache.camel.spring.boot.CamelContextConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class ConfigBeans {
    
    /*
     * ActiveMQ broker config
     */
    
    @Primary
    @Bean('jmsConnectionFactory')
    public ActiveMQConnectionFactory jmsConnectionFactory() {
        
        // supports single or comma-separated amqHost brokers
        def amqConnectionList = System.getenv('AMQ_CONNECTION_LIST')?.trim() ?: 'tcp://localhost:61616'
        String amqURLString = "failover:(${amqConnectionList})?nested.wireFormat.maxInactivityDuration=5000&jms.prefetchPolicy.all=1"
        
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory()
        connectionFactory.setBrokerURL(amqURLString)
        
        String userName = System.getenv('AMQ_USER')?.trim() ?: 'mquser'
        String password = System.getenv('AMQ_PASS')?.trim() ?: 'mqpass'
        
        connectionFactory.setUserName(userName)
        connectionFactory.setPassword(password)
        
        return connectionFactory
    }
    
    @Bean(initMethod = "start", destroyMethod = "stop")
    public PooledConnectionFactory pooledConnectionFactory() {
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory()
        pooledConnectionFactory.setMaxConnections(100)
        pooledConnectionFactory.setConnectionFactory(jmsConnectionFactory())
        return pooledConnectionFactory
    }
    
    @Bean
    public JmsConfiguration getJmsConfiguration() {
        JmsConfiguration jmsConfiguration = new JmsConfiguration()
        jmsConfiguration.setConnectionFactory(pooledConnectionFactory())
        return jmsConfiguration
    }
    
    @Bean
    public JmsConfiguration getJmsHighPriorityConfiguration() {
        JmsConfiguration jmsConfiguration = new JmsConfiguration()
        jmsConfiguration.setConnectionFactory(pooledConnectionFactory())
        jmsConfiguration.setPriority(8)
        return jmsConfiguration
    }
    
    @Bean
    public ActiveMQComponent activemq() {
        ActiveMQComponent activeMQComponent = new ActiveMQComponent(connectionFactory: pooledConnectionFactory())
        return activeMQComponent
    }
    
    //@Override
    protected void setupCamelContext(CamelContext camelContext) throws Exception {
        ActiveMQComponent activeMQComponent = new ActiveMQComponent()
        activeMQComponent.setConfiguration(getJmsConfiguration())
        camelContext.addComponent("activemq", activeMQComponent)

        ActiveMQComponent activeMQHighPriorityComponent = new ActiveMQComponent()
        activeMQHighPriorityComponent.setConfiguration(getJmsHighPriorityConfiguration())
        camelContext.addComponent("activemq-high-priority", activeMQHighPriorityComponent)
    }
    
    /*
     * Camel REST config
     */
    
    @Bean
    RestConfigBean restConfigBean(){
        RestConfigBean bean = new RestConfigBean()
        
        bean.with {
            host =      "${System.getenv('API_SERVER_ADDRESS')?.trim() ?: '0.0.0.0'}"?.toString()
            adminPort = "${System.getenv('ADMIN_SERVER_PORT')?.trim() ?: '9080'}"?.toInteger()
            apiPort =   "${System.getenv('API_SERVER_PORT')?.trim() ?: '8080'}"?.toInteger()
            protocol =  "${System.getenv('API_REST_SERVER_PROTOCOL')?.trim() ?: 'http'}"?.toString()
        }
        
        return bean
    }
    
    
    /*
     * setup for camel
     */
    
    @Autowired
    ApplicationContext appContext
    
    private static int PRODUCER_CACHE_SIZE = 100
    private static int CONSUMER_CACHE_SIZE = 100
    
    @Bean
    ProducerTemplate producerTemplate(CamelContext camelContext) {
        return camelContext.createProducerTemplate(PRODUCER_CACHE_SIZE)
    }
    
    @Bean
    ConsumerTemplate consumerTemplate(CamelContext camelContext) {
        return camelContext.createConsumerTemplate(CONSUMER_CACHE_SIZE)
    }
    
    @Bean
    CamelBeanPostProcessor camelBeanPostProcessor() {
        CamelBeanPostProcessor processor = new CamelBeanPostProcessor()
        processor.setApplicationContext(appContext)
        return processor
    }
    
    @Bean
    CamelContextConfiguration contextConfiguration() {
        return new CamelContextConfiguration() {
            @Override
            void beforeApplicationStart(CamelContext camelContext) {
                
                // prepare for graceful camel shutdown
                camelContext.getShutdownStrategy().setSuppressLoggingOnTimeout(false)
                camelContext.getShutdownStrategy().setLogInflightExchangesOnTimeout(true)
            }
            
            @Override
            void afterApplicationStart(CamelContext camelContext) {
                
            }
        }
    }
    
}



