/*
 * Created on Aug 8, 2005
 * 
 * file: ITranslator.java
 */
package ddproto1.configurator.newimpl;

import ddproto1.exception.NoSuchSymbolException;

public interface ITranslator {
    public Iterable<String> allTranslationKeys();
    public String translate(String key) throws NoSuchSymbolException;
}
