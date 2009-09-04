package com.indexdata.masterkey.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import org.apache.log4j.Logger;

/**
 * Obtains configuration based on domain (host name) and component (J2EE module).
 * Caches the configuration per component@domain.
 * Retrieves parameters per servlet.
 *
 * Throws ServletException 
 *    If the configuration file (property file) is not found
 *
 * @author nielserik
 */
public class MasterkeyConfiguration {

    public static final String MASTERKEY_CONFIG_LIFE_TIME_PARAM = "MASTERKEY_CONFIG_LIFE_TIME";

    private Logger logger = Logger.getLogger("com.indexdata.masterkey.config.");
    private static ConcurrentHashMap<String,MasterkeyConfiguration> configLocationCache = new ConcurrentHashMap();
    private boolean cacheConfigParams = true;
    private String serviceName = null;

    private ConcurrentHashMap<String,Properties> configParametersCache = new ConcurrentHashMap();
    ConfigFileLocation configFileLocation = null;

    private MasterkeyConfiguration(String serviceName, ServletContext servletContext, String hostName) throws IOException {
        cacheConfigParams = areConfigParamsCached(servletContext.getInitParameter(MASTERKEY_CONFIG_LIFE_TIME_PARAM));
        configFileLocation = new ConfigFileLocation(servletContext, hostName);
        this.serviceName = serviceName;
    }

    private Logger getLogger() {
        return logger;
    }

    /**
     * Creates a singleton MasterkeyConfiguration for each combination of component and host name.
     * The instance acts as a cache for the location of the specific config file, but is not
     * a cache for the configuration parameters as such.
     * In other words: The location of the config file for a given component in a given host is
     * retrieved once in the servlets life time, how often the configuration parameters are retrieved
     * is configurable
     * @param serviceName An identifier for this configuration. Must match the prefix for the properties in the property file, 
     *                    like AuthServlet.my_property or RecordsResource.my_property
     *                    Is typically a Servlet name but if identification beyond Servlet name
     *                    is required - i.e. for REST services - then any other identifier can be used.
     * @param servletContext Needed to pick up init parameters regarding the location of config files
     * @param hostName Used for resolving the path to config files.
     */
    public static MasterkeyConfiguration getInstance (String serviceName, ServletContext servletContext, String hostName) throws IOException {
        MasterkeyConfiguration cfg = null;
        String cfgKey = servletContext.getContextPath() + "/" + serviceName +"@"+hostName;
        if (configLocationCache.containsKey(cfgKey)) {
            cfg = (MasterkeyConfiguration) (configLocationCache.get(cfgKey));
            cfg.getLogger().debug("Returning cached config location for '" + cfgKey + "': '" + cfg.getConfigFileLocation().getConfigFilePath() + "'");
        } else {            
            cfg = new MasterkeyConfiguration(serviceName, servletContext, hostName);
            cfg.getLogger().debug("No previously cached config location reference found for '" + cfgKey + "'. Instantiating a new config location reference: '" + cfg.getConfigFileLocation().getConfigFilePath() + "'");
            // Check that config file is readable, if not, analyze and throw exception
            cfg.getConfigFileLocation().evaluate();
            // The file location passed tests, store it
            configLocationCache.put(cfgKey, cfg);
        }
        return cfg;
    }
    
    
    /**
     * Sets the life time of configuration parameters to 'servlet' (cache=true) or 'request' (cache=false
     * @param configLifeTime
     * @return
     * @throws javax.servlet.ServletException
     */
    private boolean areConfigParamsCached(String configLifeTime) {
        boolean setting = true;
        if (configLifeTime == null || configLifeTime.length() == 0) {
            logger.warn(MASTERKEY_CONFIG_LIFE_TIME_PARAM + " init parameter not defined in deployment descriptor. Can be 'REQUEST' or 'SERVLET'. Defaulting to 'SERVLET'.");
        } else {
            if (configLifeTime.equalsIgnoreCase("REQUEST")) {
                setting = false;
            } else {
                if (!configLifeTime.equalsIgnoreCase("SERVLET")) {
                    logger.warn(MASTERKEY_CONFIG_LIFE_TIME_PARAM + " init parameter can be one of 'REQUEST' or 'SERVLET'. Was '" + configLifeTime + "'. Defaulting to 'SERVLET'.");
                }
            }
        }
        return setting;
    }


    /**
     * Retrieves all config parameter names for the servlet
     * @return
     * @throws javax.servlet.ServletException
     */
    private Enumeration getConfigParameterNames(String prefix) throws IOException {
        Properties prop = getComponentProperties(configFileLocation.getConfigFilePath());
        Hashtable<String, String> keyList = new Hashtable<String, String>();
        Iterator keysIter = prop.keySet().iterator();
        int i = 0;
        while (keysIter.hasNext()) {
            String key = (String) keysIter.next();
            if (key.startsWith(prefix)) {
                key = key.replace(prefix + ".", "");
                keyList.put("" + (i++), key);
            }
        }
        return keyList.elements();
    }

    /**
     * Retrieves all config paramers prefixed with the given service name (ie the servlet name) 
     * @return
     * @throws IOException
     */
    private Enumeration getConfigParameterNames() throws IOException {
        Properties prop = getComponentProperties(configFileLocation.getConfigFilePath());
        Hashtable<String, String> keyList = new Hashtable<String, String>();
        Iterator keysIter = prop.keySet().iterator();
        int i = 0;
        while (keysIter.hasNext()) {
            String key = (String) keysIter.next();
            if (key.startsWith(serviceName)) {
                key = key.replace(serviceName + ".", "");
                keyList.put("" + (i++), key);
            }
        }
        return keyList.elements();
    }
    
    
    /**
     * Retrieves a given init parameter that applies to the invoking Servlet.
     * @param name
     * @return
     * @throws javax.servlet.ServletException
     */
    public String getConfigParameter(String prefix, String name) throws IOException {
        Properties prop = getComponentProperties(configFileLocation.getConfigFilePath());
        String propertyValue = (String) prop.get(prefix + "." + name);
        if (propertyValue == null || propertyValue.length() == 0) {
            logger.error("Could not find value for key '" + name + "'");
        } else {
            logger.debug("Found value '" + propertyValue + "' for key '" + name + "'");
        }
        return propertyValue;
    }

    /**
     * Retrieves a given init parameter prefixed with a given service name (ie a servlet name) 
     * @param name  The name of the parameter, without the service name prefix.
     * @return
     * @throws javax.servlet.ServletException
     */
    public String getConfigParameter(String name) throws IOException {
        Properties prop = getComponentProperties(configFileLocation.getConfigFilePath());
        String propertyValue = (String) prop.get(serviceName + "." + name);
        if (propertyValue == null || propertyValue.length() == 0) {
            logger.error("Could not find value for key '" + name + "'");
        } else {
            logger.debug("Found value '" + propertyValue + "' for key '" + name + "'");
        }
        return propertyValue;
    }
    

    /**
     * Retrieves properties (configuration) for a given J2EE module
     * Will cache the properties if so specified in the deployment descriptor
     * @param configFilePath
     * @return
     * @throws javax.servlet.ServletException
     */
    private Properties getComponentProperties(String configFilePath) throws IOException {
        Properties componentProperties = null;
        if (cacheConfigParams && configParametersCache.containsKey(configFilePath)) {
            componentProperties = configParametersCache.get(configFilePath);
            logger.debug("Found cached properties for '" + configFilePath + "'");
        } else {
            componentProperties = new Properties();
            try {
                componentProperties.load(new FileInputStream(configFileLocation.getConfigFilePath()));
                logger.debug("Loaded properties from file system using '" + configFilePath + "'");
            } catch (FileNotFoundException fnfe) {
                logger.error(fnfe + "Could not find property file '" + configFilePath + "'");
                configFileLocation.evaluate();
            } catch (IOException ioe) {
                logger.error(ioe + "Could not load property file '" + configFilePath + "'");
                throw new IOException("Could not load property file '" + configFilePath + "'" + ioe.getMessage());
            }
            if (cacheConfigParams) {
               configParametersCache.put(configFilePath, componentProperties);
            }
        }
        return componentProperties;
    }

    /**
     * Retrieves all servlet init parameters as a HashMap. This is the way the
     * Pazpar2 proxy factory likes to get its arguments.
     *
     * @return
     * @throws javax.servlet.ServletException
     */
    public Map<String, String> getConfigParamsAsMap(String prefix) throws IOException {
        Map<String, String> paramMap = new HashMap<String, String>();
        Enumeration paramNames = getConfigParameterNames(prefix);
        while (paramNames.hasMoreElements()) {
            String paramName = (String) paramNames.nextElement();
            paramMap.put(paramName, getConfigParameter(prefix, paramName));
        }
        return paramMap;
    }

    public void writeTemplatePropertyFile () throws ServletException {
        
    }

    private ConfigFileLocation getConfigFileLocation () {
        return configFileLocation;
    }

}
