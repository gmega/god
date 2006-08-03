/*
 * Created on Apr 3, 2005
 * 
 * file: UnorderedMultiMap.java
 */
package ddproto1.util.collection;

import java.util.Collection;

/**
 * MultiMap with unordered chains. Uses Collection interface.
 * 
 * Don't trust it under multiple threads, please.
 */

public class UnorderedMultiMap <K, V> extends AbstractMultiMap <K, V>  
{
    private Class <? extends Collection> colType;
    
    public UnorderedMultiMap(Class <? extends Collection> collectionType) {
        this.colType = collectionType;
    }

    @Override
    protected Class <? extends Collection> getConcreteType() {
        return colType;
    }
}
