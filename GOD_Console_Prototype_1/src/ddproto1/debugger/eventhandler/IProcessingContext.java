/*
 * Created on Jan 11, 2005
 * 
 * file: ProcessingContext.java
 */
package ddproto1.debugger.eventhandler;

/**
 * @author giuliano
 *
 */
public interface IProcessingContext {
    public void vote(String voteType);
    public int getResults(String voteType);
}
