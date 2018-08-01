package com.microservice

class RequiredEnvVars {
    public final static def serviceEnvironmentVariableList = [
        [key: 'TERM', required: false],
        [key: 'TERM_SESSION_ID', required: false],
        [key: 'spring.output.ansi.console-available', required: false],
        [key: 'TMPDIR', required: false],
        [key: 'USER', required: false],
        
        [key: 'API_SERVER_ADDRESS', required: true],
        [key: 'API_SERVER_PORT', required: true],
        [key: 'ADMIN_SERVER_PORT', required: true],
        [key: 'API_REST_SERVER_PROTOCOL', required: true],
        
        [key: 'AMQ_CONNECTION_LIST', required: true],
        [key: 'AMQ_USER', required: true],
        [key: 'AMQ_PASS', required: true],
        
        [key: 'ENABLE_CONSUMER_PRODCER_TEST', required: true],
        [key: 'ENABLE_BENCHMARK_TEST', required: true],
        [key: 'BENCHMARK_THREADS', required: true],
        [key: 'BENCHMARK_TOTAL_MSGS', required: true],
        [key: 'ENABLE_REQUEST_REPLY_TEST', required: true],
    ]
}
