/*
 * Created on Oct 19, 2005
 * 
 * file: CharacterStuffer.java
 */
package ddproto1.util;

import ddproto1.exception.commons.UnsupportedException;

public class Base64Encoder implements IStringEncoder{
    
    public Base64Encoder(){ }

    public void setDisallowedCharacters(String[] ss) 
    {
        for(String s : ss){
            if(s.length() > 1) throw new UnsupportedException("Disallowed characters must " +
                    "be one character strings");
            if(s.matches("([a-zA-Z]|[+/=])"))
                throw new UnsupportedException("Cannot remove character " + s
                        + " from Base64 encoding.");
        }
    }

    public String encode(String s) {
        return Base64.encodeBytes(s.getBytes());
    }

    public String decode(String s) {
        return new String(Base64.decode(s));
    }
}
