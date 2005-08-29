/*
 * Created on Aug 3, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: SourceMapper.java
 */

package ddproto1.sourcemapper;

import java.io.IOException;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.Location;

/**
 * @author giuliano
 *
 */
public interface ISourceMapper {
    public String getLine(Location loc);
    public void setSourceFactory(SourceFactory sf);
    public ISource getSource(Location loc) throws AbsentInformationException,
            IOException;
}
