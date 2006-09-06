/*
 * Created on Aug 3, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: SourceFactory.java
 */

package ddproto1.sourcemapper;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author giuliano
 * @deprecated
 */
public interface SourceFactory {
    public ISource make(InputStream srcstream, String name)
    	throws IOException;
}
