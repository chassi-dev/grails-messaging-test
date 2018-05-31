package com.microservice

import org.apache.activemq.camel.component.ActiveMQComponent
import org.apache.activemq.jms.pool.PooledConnectionFactory
import org.apache.activemq.spring.ActiveMQConnectionFactory
import org.apache.camel.CamelContext
import org.apache.camel.component.jms.JmsConfiguration
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
        String brokerURL = "tcp://${System.getenv('AMQ_HOST')?.trim() ?: '0.0.0.0'}:${System.getenv('AMQ_PORT')?.trim() ?: '61616'}"
        
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(brokerURL);
        
        String userName = System.getenv('AMQ_USER')?.trim() ?: 'mquser'
        String password = System.getenv('AMQ_PASS')?.trim() ?: 'mqpass'
        
        connectionFactory.setUserName(userName)
        connectionFactory.setPassword(password)
        
        return connectionFactory;
    }
    
    @Bean(initMethod = "start", destroyMethod = "stop")
    public PooledConnectionFactory pooledConnectionFactory() {
        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setMaxConnections(10);
        pooledConnectionFactory.setConnectionFactory(jmsConnectionFactory());
        return pooledConnectionFactory;
    }
    
    @Bean
    public JmsConfiguration getJmsConfiguration() {
        JmsConfiguration jmsConfiguration = new JmsConfiguration();
        jmsConfiguration.setConnectionFactory(pooledConnectionFactory());
        return jmsConfiguration;
    }
    
    @Bean
    public JmsConfiguration getJmsHighPriorityConfiguration() {
        JmsConfiguration jmsConfiguration = new JmsConfiguration();
        jmsConfiguration.setConnectionFactory(pooledConnectionFactory());
        jmsConfiguration.setPriority(8);
        return jmsConfiguration;
    }
    
    @Bean
    public ActiveMQComponent activemq() {
        ActiveMQComponent activeMQComponent = new ActiveMQComponent(connectionFactory: jmsConnectionFactory())
        return activeMQComponent
    }
    
    //@Override
    protected void setupCamelContext(CamelContext camelContext) throws Exception {
        ActiveMQComponent activeMQComponent = new ActiveMQComponent();
        activeMQComponent.setConfiguration(getJmsConfiguration());
        camelContext.addComponent("activemq", activeMQComponent);

        ActiveMQComponent activeMQHighPriorityComponent = new ActiveMQComponent();
        activeMQHighPriorityComponent.setConfiguration(getJmsHighPriorityConfiguration());
        camelContext.addComponent("activemq-high-priority", activeMQHighPriorityComponent);
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

}



