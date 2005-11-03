/*
 * Created on Oct 19, 2005
 * 
 * file: ICharacterEncoder.java
 */
package ddproto1.util;

public interface IStringEncoder {
    public void setDisallowedCharacters(String [] s);
    public String encode(String s);
    public String decode(String s);
}
