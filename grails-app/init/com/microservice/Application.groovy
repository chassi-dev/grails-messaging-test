package com.microservice

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.system.ApplicationPidFileWriter
import org.springframework.context.annotation.ComponentScan

@ComponentScan
class Application extends GrailsAutoConfiguration {
	@Autowired
	ConfigBeans configBeans
	
    static void main(String[] args) {
		final GrailsApp app = new BannerGrailsApp(Application)
		
        app.banner = new GroovyBanner()
		
		// stop calling out to terracotta.org
		System.setProperty("net.sf.ehcache.skipUpdateCheck", "true")
		
		// fail fast
		System.setProperty("spring.pid.fail-on-write-error", "true")
		
		System.setProperty("logging.name", "application.log")
		
		//System.setProperty("pid.folder", ".")
		System.setProperty("spring.pid.file", "application.pid")
		System.setProperty("pidFilename", "application.pid")
		System.setProperty("PIDFilename", "application.pid")
		System.setProperty("pid.file", "application.pid")
		System.setProperty("spring.pidfile", "application.pid")
		
		// Register PID file writer
		app.addListeners(new ApplicationPidFileWriter())
		
		
		app.run()
    }
}
