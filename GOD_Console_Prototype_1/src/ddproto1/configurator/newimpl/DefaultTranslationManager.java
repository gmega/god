/*
 * Created on Aug 3, 2005
 * 
 * file: DefaultTranslationManager.java
 */
package ddproto1.configurator.newimpl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ddproto1.exception.NoSuchSymbolException;

public class DefaultTranslationManager implements ITranslationManager{

    private List <String> classes = new ArrayList<String>();
    private ClassTuple lookupTuple = new ClassTuple(null, null);
    private Map<ClassTuple, Map<String,String>> dictionaries = new HashMap<ClassTuple, Map<String,String>>();
    private String base;
    
    private static DefaultTranslationManager instance = null;
    
    public static synchronized ITranslationManager getInstance()
        throws IOException
    {
        return (instance == null)?(instance = new DefaultTranslationManager()):instance;
    }
    
    private DefaultTranslationManager() 
        throws IOException
    {
        /** lookup directory defaults to user.dir */
        String nBase = System.getProperty("user.dir");
        if(!nBase.endsWith(File.separator)) nBase += File.separator;
        nBase += IConfigurationConstants.SPECS_DIR;
        this.setLookupDirectory(nBase);
    }

    public void setLookupDirectory(String base)
        throws IOException
    {
        if(!base.endsWith(File.separator)) base += File.separator;
        
        /** Loads the new class table */
        this.base = base;
        
        File tToc = new File(this.base + IConfigurationConstants.TRANSLATION_TOC_FILENAME);
        if (!tToc.exists())
            throw new FileNotFoundException(
                    "Could not locate the translation table of contents in directory "
                            + base);
        
        BufferedReader in = new BufferedReader(new FileReader(tToc));
        
        String cLine = null;
        classes = new ArrayList<String>();
        while(!((cLine = in.readLine()) == null)) classes.add(cLine);
    }
    
    public String translate(String key, Class parent, Class child) 
        throws NoSuchSymbolException
    {
        lookupTuple.setParent(parent);
        lookupTuple.setChild(child);
        
        if(!dictionaries.containsKey(lookupTuple))
            this.loadTranslation(lookupTuple);
        
        return (dictionaries.get(lookupTuple).get(key));
    }

    private void loadTranslation(ClassTuple tuple) throws NoSuchSymbolException{
        String name1 = tuple.getParent().getCanonicalName();
        String name2 = tuple.getChild().getCanonicalName();
        
        int cIdx1 = classes.indexOf(name1);
        int cIdx2 = classes.indexOf(name2);
        
        if(cIdx1 == -1 || cIdx2 == -1)
            throw new NoSuchSymbolException(
                    "Could not find one of the following classes in the translation class table:\n "
                            + name1 + " " + name2);
                        
        String expectedName = cIdx1 + "-" + cIdx2 + "."
                + IConfigurationConstants.TRANSLATION_FILE_EXTENSION;
        
        File translationFile = new File(base + expectedName);
        
        if (!translationFile.exists())
            throw new NoSuchSymbolException(
                    "Translation file mapping attributes from class " + name1
                            + " to class " + name2 + " could not be found.");
        
        
                
    }
    
    private class ClassTuple{
        private Class c1;
        private Class c2;
        
        public ClassTuple(Class c1, Class c2){
            this.c1 = c1;
            this.c2 = c2;
        }
        
        public ClassTuple(ClassTuple tuple){
            this.setParent(tuple.getParent());
            this.setChild(tuple.getChild());
        }
        
        public void setParent(Class parent){
            this.c1 = parent;
        }
     
        public void setChild(Class child){
            this.c2 = child;
        }
        
        public Class getParent(){
            return c1;
        }
        
        public Class getChild(){
            return c2;
        }
        
        public boolean equals(Object o){
            if(!(o instanceof ClassTuple)) return false;
            
            ClassTuple other = (ClassTuple)o;
            return(other.c1.equals(c1) && other.c2.equals(c2));
        }
        
        public int hashCode(){
            return c1.hashCode() + c2.hashCode();
        }
        
    }
}
