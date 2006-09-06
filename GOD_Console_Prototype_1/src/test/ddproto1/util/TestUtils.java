/*
 * Created on Jul 3, 2006
 * 
 * file: TestUtils.java
 */
package ddproto1.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.Bundle;

import ddproto1.GODBasePlugin;
import ddproto1.configurator.ConfiguratorUtils;

public class TestUtils implements TestConfigurationConstants{

    private static volatile boolean pTest = false;
    
    /** Properties are thread-safe, even though that's not documented 
     * as of Tiger. */
    private static final AtomicReference<DelayedResult<Properties>> props = 
        new AtomicReference<DelayedResult<Properties>>(new DelayedResult<Properties>());
    
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    
    public static void setPluginTest(boolean plTest){
        pTest = plTest;
    }
    
    public static boolean isPluginTest(){
        return pTest;
    }
    
    public static URL getResource(String subpath)
        throws MalformedURLException, InterruptedException, ExecutionException
    {
        if (!pTest)
            return new URL(TestUtils.getProperty(TestUtils.BASEDIR_URL) + "/"
                    + subpath);
        
        Bundle bundle = GODBasePlugin.getDefault().getBundle();
        return bundle.getEntry("/" + subpath);
    }
    
    private static Properties getProperties()
        throws ExecutionException, InterruptedException
    {
        DelayedResult<Properties> rProps = props.get();
        
        if(initialized.compareAndSet(false, true)){
            /** If a thread gets here, it might be because 
             * a previous load has failed (see below). This
             * means we have to refresh the delayed result
             * reference because it might have changed.
             */
            rProps = props.get();
            /** This refers to the test.properties file, under src/test/resources. */
            InputStream conf = getResource("test.properties", TEST_PROPS_URL);
            /** This refers to the bootstrap properties. */
            InputStream vars = getResource("bootstrap.properties", BSTRAP_PROPS_URL);
            InputStream absoluteVars = getResource("absolute.test.properties", ABS_PROPS_URL);
            
            Properties confProperties = new Properties();
            Properties varProperties = new Properties();
            Properties absoluteProperties = new Properties();
            try{
                absoluteProperties.load(absoluteVars);
                varProperties.load(vars);
                for(Object key : absoluteProperties.keySet()){
                    varProperties.put(key, (String)absoluteProperties.get(key));
                }
                String newConf = ConfiguratorUtils.tokenReplace(conf, varProperties);
                confProperties.load(new ByteArrayInputStream(newConf.getBytes()));
                rProps.set(confProperties);
            }catch(Exception exe){
                // Load failed. Set initialized to false and 
                // refresh the future to avoid poisoning.
                props.get().setException(exe);
                props.set(new DelayedResult<Properties>());
                initialized.set(false);
            }
        }
        
        return rProps.get();
    }
    
    private static InputStream getResource(String name, String prop)
        throws ExecutionException
    {
        InputStream conf = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(name);
        Exception ex = null;
        if(conf == null){
            String url = System.getProperty(prop);
            if(url != null){
                try{
                    conf = new URL(url).openStream();
                }catch(Exception exe){
                    ex = exe;
                }
            }
            
            if(conf == null){
                ExecutionException exec = new ExecutionException("Resource doesn't exist. Is your classpath correct?", ex);
                props.get().setException(exec);
                props.set(new DelayedResult<Properties>());
                initialized.set(false);
                throw exec;
            }
        }
        
        return conf;

    }
    
    public static String getProperty(String prop)
        throws ExecutionException, InterruptedException
    {
        
        String propVal = getProperties().getProperty(prop);
        if(propVal == null)
            throw new RuntimeException("Property " + prop + " not found.");
        
        return propVal;
    }
    
    public static Properties getTestPropertiesCopy()
        throws ExecutionException, InterruptedException
    {
        return (Properties)getProperties().clone();
    }
    
}
