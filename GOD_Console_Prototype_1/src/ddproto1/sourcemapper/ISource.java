/*
 * Created on Aug 3, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: Source.java
 */

package ddproto1.sourcemapper;

/**
 * @author giuliano
 *
 */
public interface ISource {
    public String getSourceName();
    public String getLine(int no);
}
