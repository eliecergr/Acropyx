// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if(System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.project.groupId = appName // change this to alter the default package name and Maven publishing destination
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]

// URL Mapping Cache Max Size, defaults to 5000
//grails.urlmapping.cache.maxsize = 1000

// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// whether to install the java.util.logging bridge for sl4j. Disable for AppEngine!
grails.logging.jul.usebridge = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []

// request parameters to mask when logging exceptions
grails.exceptionresolver.params.exclude = ['password']

grails.app.context = "/"

// log4j configuration
log4j = {
    root {
        info 'stdout'
    }

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'
           
    warn   'org.mortbay.log'
}

// Added by the Spring Security Core plugin:
grails.plugins.springsecurity.userLookup.userDomainClassName = 'ch.acropyx.AcroUser'
grails.plugins.springsecurity.userLookup.authorityJoinClassName = 'ch.acropyx.UserRole'
grails.plugins.springsecurity.authority.className = 'ch.acropyx.Role'
//grails.plugins.springsecurity.rejectIfNoRule = true
grails.plugins.springsecurity.voterNames = [ 'authenticatedVoter', 'roleVoter', 'multiTenantVoter' ]

ch.acropyx.dateFormat = 'dd/MM/yyyy HH:mm'
ch.acropyx.dateFormats = ['dd/MM/yyyy HH:mm', 'dd/MM/yyyy']
ch.acropyx.integerFormat = '#'

// Send json rpc order to localhost
ch.acropyx.displayerPort = 80
//ch.acropyx.displayerPort = 3000
ch.acropyx.displayerUrl = 'http://localhost:' + ch.acropyx.displayerPort
//ch.acropyx.displayerUrl = 'http://displayer.acropyx.com:' + ch.acropyx.displayerPort
environments {
    production {
        tenant {
            mode = "multiTenant"
            resolver.type = "request"
            resolver.request.dns.type = "config"
            domainTenantMap = [:]
//            domainTenantMap.put("46.38.243.67", 1)
            domainTenantMap.put("acropyx.com", 1)
            //domainTenantMap.put("epyx.acropyx.com", 1)
            //domainTenantMap.put("gilles.acropyx.com", 2)
            //domainTenantMap.put("claudio.acropyx.com", 3)
            //domainTenantMap.put("cs2011.acropyx.com", 4)
            //domainTenantMap.put("admin.acropyx.com",5)
            //domainTenantMap.put("displayer.acropyx.com",5)
        }
    }
    development {
        tenant {
            mode = "multiTenant"
            resolver.type = "request"
            resolver.request.dns.type = "config"
            domainTenantMap = [:]
           // domainTenantMap.put("46.38.243.67", 1)
            domainTenantMap.put("acropyx.com", 1)
         //   domainTenantMap.put("test2.localhost", 3)
            //domainTenantMap.put("displayer.acropyx.com",1)
            
        }
    }
    test {
        tenant {
            mode = "multiTenant"
            resolver.type = "request"
            resolver.request.dns.type = "config"
            domainTenantMap = [:]
            domainTenantMap.put("localhost", 1)
        }
    }
}
