/*
 * Created on Aug 3, 2004
 * 
 * Distributed Debugger Prototype 1
 * 
 * File: SourceCache.java
 */

package ddproto1.sourcemapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


/**
 * @author giuliano
 *
 * @deprecated
 */
public class SourceCache implements SourceFactory{
            
    public SourceCache(){ }
    
    public ISource make(InputStream is, String name)
    	throws IOException
    {
        CachedSource cs = new CachedSource(is, name);
        return cs;
        
    }
    
    public class CachedSource implements ISource{
        
        private ArrayList src = new ArrayList();
        private String name;
        
        protected CachedSource(InputStream is, String srcname) 
        	throws IOException
        {
            this.name = srcname;
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            
            String line = null;
            
            while((line = br.readLine()) != null){
                src.add(line);
            }
        }
        
        public String getLine(int number){
            if(number >= src.size() || number < 0)
                return null;
            
            return (String)src.get(number - 1);
        }
        
        public String getSourceName(){
            return name;
        }
    }

}
