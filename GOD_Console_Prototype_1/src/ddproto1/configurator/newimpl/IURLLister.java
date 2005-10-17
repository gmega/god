/*
 * Created on Oct 15, 2005
 * 
 * file: IURLLister.java
 */
package ddproto1.configurator.newimpl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.IllegalFormatException;
import java.util.List;

public interface IURLLister {
    public URL       getEntry (URI source, String entryName) throws MalformedURLException;
    public List<URL> list     (URI source)                   throws IllegalProtocolException;
}
