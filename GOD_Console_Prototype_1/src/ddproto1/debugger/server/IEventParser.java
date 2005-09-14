/*
 * Created on Sep 27, 2004
 * 
 * file: IEventParser.java
 */
package ddproto1.debugger.server;

import ddproto1.util.commons.ByteMessage;
import ddproto1.util.commons.Event;
import ddproto1.exception.commons.ParserException;

/**
 * @author giuliano
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public interface IEventParser {
    public Event parse(ByteMessage bm) throws ParserException;
}
