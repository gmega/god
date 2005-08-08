/*
 * Created on Aug 3, 2005
 * 
 * file: ITranslationManager.java
 */
package ddproto1.configurator.newimpl;

import ddproto1.exception.NoSuchSymbolException;

public interface ITranslationManager {
    public ITranslator translatorFor(Class from, Class to) throws NoSuchSymbolException;
}
