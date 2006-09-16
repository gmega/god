/*
 * Created on 16/09/2006
 * 
 * file: ConfigurationDecorator.java
 */
package ddproto1.controller.remote.test;

import org.apache.log4j.BasicConfigurator;

import junit.framework.TestCase;

public class ConfigurationDecorator extends TestCase {
    public void testSetup(){
        BasicConfigurator.configure();
    }
}
