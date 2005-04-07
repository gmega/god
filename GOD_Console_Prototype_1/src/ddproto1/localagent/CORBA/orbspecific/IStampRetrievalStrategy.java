/*
 * Created on Sep 20, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: IStampRetrievalStrategy.java
 */

package ddproto1.localagent.CORBA.orbspecific;

import java.util.Iterator;

import ddproto1.localagent.Tagger;

/**
 * This interface represents an attempt to decouple the stamp recovery
 * strategy (which is related to how the ORB implements its PICurrents)
 * and the rest of the program. 
 * 
 * The implementor may assume that all relevant CurrentSpecs will be passed
 * as parameters through the Iterator and that the "correct" PICurrent 
 * contains the desired thread id. 
 * 
 * Note: This operation might not be performable in some ORB implementations -
 * if that ever happens, then the debugger will need a redesign to support 
 * that ORB. 
 * 
 * @author giuliano
 *
 */
public interface IStampRetrievalStrategy {
    public String retrieve(String retrieve, Iterator piCurrents, Tagger t) throws Exception;
}
