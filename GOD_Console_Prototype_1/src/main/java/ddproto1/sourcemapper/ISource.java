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
 * @deprecated No longer in use. We use the Eclipse source-mapping
 * interfaces now.
 */
public interface ISource {
    public String getSourceName();
    public String getLine(int no);
}
