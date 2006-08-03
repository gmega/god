/*
 * Created on 24/07/2006
 * 
 * file: ConfiguratorUtils.java
 */
package ddproto1.configurator;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.xml.sax.SAXException;

public class ConfiguratorUtils {
    public static String tokenReplace(InputStream is, Properties props)
            throws SAXException {
        VelocityContext vc = new VelocityContext();
        InputStreamReader reader = new InputStreamReader(is);

        for (Object key : props.keySet())
            vc.put(key.toString(), props.get(key).toString());

        VelocityEngine ve = new VelocityEngine();
        StringWriter writer = new StringWriter();

        try {
            ve.init();
            ve.evaluate(vc, writer, XMLConfigurationParser.class.getName(),
                    reader);
        } catch (Exception ex) {
            throw new SAXException(
                    "Error while processing document properties.", ex);
        }

        return writer.toString();
    }
}
