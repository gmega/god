/*
 * Created on Feb 1, 2005
 * 
 * file: GroupedRequestFactory.java
 */
package ddproto1.debugger.managing.distributed;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import ddproto1.debugger.request.IDeferrableRequest;
import ddproto1.exception.ConfigException;
import ddproto1.exception.NoSuchSymbolException;
import ddproto1.exception.NestedException;

/**
 * @author giuliano
 *
 */
public class GroupedRequestFactory {
    
    private Map classRepository = new HashMap();
    
    public GroupedRequestFactory(){ }
    
    public IGroupedRequest makeGroupedRequestFor(Map req)
    	throws NoSuchSymbolException, NestedException
    {
        Class group = (Class)classRepository.get(req.getClass());
        if(group == null){
            throw new NoSuchSymbolException("There's no group" +
            		" request registered for single request class " 
                    + req.getClass().toString());
        }
        
        Constructor cons;
        try{
            cons = group.getConstructor(new Class [] {Map.class});
            IGroupedRequest greq = (IGroupedRequest)cons.newInstance(new Object [] {req});
            return greq;
        }catch(NoSuchMethodException e){
            throw new InternalError("Could not find required constructor for class " + group.toString());
        }catch(Exception e){
            throw new NestedException("Failed to instantiate a grouped request. ", e);
        }
        
        
    }
    
    public void registerGroupedRequest(Class req, Class group)
    	throws ConfigException{

        /* Checks if the request class is actually a request class */
        if(!IDeferrableRequest.class.isAssignableFrom(req)){
            throw new ConfigException(
                    "Grouped requests can only be built from "
                            + "classes implementing interface "
                            + IDeferrableRequest.class.toString());
        }

        /* Checks if the group request class obbeys the required rules */
        if (!IGroupedRequest.class.isAssignableFrom(group)) {
            throw new ConfigException("Grouped requests must implement "
                    + "interface " + IGroupedRequest.class.toString());
        }
        
        try{
            Constructor con = group.getConstructor(new Class [] {Map.class});
        }catch(NoSuchMethodException e){
            throw new ConfigException(
                    "Class "
                            + group.toString()
                            + " must have a constructor that takes a " +
                             	"Map as parameter.");
        }
       
        
        classRepository.put(req, group);                
    }
    
}
