/*
 * Created on Sep 11, 2005
 * 
 * file: IValuePrinter.java
 */
package ddproto1;

import com.sun.jdi.Value;

import ddproto1.interfaces.IMessageBox;

public interface IValuePrinter {
    public void printValue(Value val, IMessageBox mbox);
}
