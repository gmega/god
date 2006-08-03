/*
 * Created on Oct 21, 2005
 * 
 * file: IConfigurationManager.java
 */
package ddproto1.configurator.plugin;

import ddproto1.configurator.IObjectSpecDecoder;
import ddproto1.configurator.IObjectSpecEncoder;
import ddproto1.configurator.ISpecLoader;
import ddproto1.exception.commons.IllegalAttributeException;
import ddproto1.exception.commons.InvalidAttributeValueException;
import ddproto1.exception.commons.UninitializedAttributeException;

/**
 * This interface gives access to the most important parts of the 
 * configurator module. 
 * 
 * @author giuliano
 */
public interface IConfigurationManager {

    /**
     * Returns a reference to the plugin's node list manager 
     * (see the INodeList interface docs for more information).
     * 
     * @see ddproto1.configurator.plugin.INodeList
     * @return
     */
    public INodeList getNodelist();

    /**
     * Returns an IObjectSpec encoder (an object capable of 
     * transforming an IObjectSpec into a String). 
     * 
     * @see ddproto1.configurator.IObjectSpecEncoder
     * @see ddproto1.configurator.IObjectSpec
     * 
     * @return
     */
    public IObjectSpecEncoder getEncoder();
    
    /**
     * Returns an IObjectSpec decoder (an object capable of 
     * a string into an IObjectSpec).
     * 
     * @see ddproto1.configurator.IObjectSpecDecoder
     * @see ddproto1.configurator.IObjectSpec
     * 
     * @return
     */
    public IObjectSpecDecoder getDecoder();

    /**
     * Returns the spec loader for the currently deployed
     * component specifications.
     * 
     * @see ddproto1.configurator.ISpecLoader
     * @see ddproto1.configurator.IObjectSpecType
     * @see ddproto1.configurator.IObjectSpec
     * 
     * @return
     */
    public ISpecLoader getSpecLoader();

    /**
     * Returns an implementation scanner for the current deployed
     * component specifications. The visibility of the implementation
     * scanner matches the visibility of the ISpecLoader instance 
     * returned by method <i>getSpecLoader</i> above. 
     * 
     * This makes me wonder if implementation scanners and ISpecLoaders 
     * shouldn't be coalesced.
     * 
     * @return
     */
    public IImplementationScanner getImplementationScanner();
}
