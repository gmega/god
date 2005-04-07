/*
 * Created on Jul 22, 2004
 *
 */
package ddproto1.launcher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ddproto1.exception.CommException;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.LauncherException;
import ddproto1.exception.NestedRuntimeException;
import ddproto1.exception.UnsupportedException;
import ddproto1.util.MessageHandler;
import ddproto1.util.traits.ConversionTrait;


/**
 * @author giuliano
 *
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class JVMShellLauncher implements IApplicationLauncher {

    private String classpath;
    private String mainclass;
    private String jvmport;
    private String tunnelclass;
    private String ttype;
    private String vmParameters = "";
    private String gid;
    private String globalAddress;
    private String appParameters;
    
    private Boolean block;
      
    private Map <String, String> tunnelAttributes = new HashMap<String, String>();
    
    private static final String module = "JVMShellLauncher -";
    
    private static final String [] attributes = {"main-class", "jdwp-port", "classpath", "tunnel-class", "vm-parameters", "gid", "global-agent-address"};
    
    /* (non-Javadoc)
     * @see ddproto1.interfaces.ApplicationLauncher#launch()
     */
    public void launch() throws CommException, LauncherException, ConfigException {
        try{
            if((classpath == null) || (mainclass == null) || (jvmport == null)
                    || (tunnelclass == null) || (gid == null) || (appParameters == null) 
                    || (block == null))
                throw new ConfigException(module + " Required launcher parameter is missing.");
            
            Class c = Class.forName(tunnelclass);
            
            IShellTunnel sht = (IShellTunnel)c.newInstance();
            passParameters(sht);
            
            /* Remove all line breaks */
            /* This is REALLY ugly. We have a particular pattern for processing strings
             * that fit a very particular case. My case. Other whitespace patterns, for
             * example, could completely break the launcher. 
             */
            classpath = classpath.replaceAll("\n", "");
            classpath = classpath.replaceAll(":( )+", ":");
            
            // FIXME This is non-portable code (works on Linux only).
            // TODO Maybe we should add an option that allows the user to choose between using nohup and
            // maintaining his tunnel connection open.
            // FIXME Another issue arises if the JVM don't return control to the command prompt (if we are in Windows, for instance).
            String script = (block == true)?"./open_block.sh":"./open.sh";
            String cmdline = script
                    + " java -cp \""
                    + classpath +"\""
                    + " -Xdebug"
                    + " -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address="
                    + jvmport + " " + vmParameters 
                    + " -Dagent.local.gid=" + gid 
                    + " -Dagent.global.address=" + globalAddress 
                    + " ddproto1.localagent.LocalLauncher --main-class=" + mainclass 
                    + " --app-parameters " + appParameters + "\n";
            
            System.err.println("Inet Address:" + globalAddress);
            
            //FIXME This part locks up like hell. We've got to add some timeout support here.
            sht.open();
 
            if(block){
                CommandAndClose cac = new CommandAndClose(sht, cmdline);
                Thread asyncExecute = new Thread(cac);
                asyncExecute.start();
            }else{
                sht.feedCommand(cmdline);
                sht.close(false);
            }
            
        
        }catch(CommException e){
            throw e;
        }catch(ConfigException e){
            throw e;
        }catch(Exception e){
            /* This isn't the only intended use for LauncherException. It should
             * be actually used when tunnel generates answers that are unexpected 
             * or when the remote system cannot launch the JVM for some reason. 
             * This version of the launcher, however, isn't clever enough.
             */
            wrap(e);
        }
    }

    /* (non-Javadoc)
     * @see ddproto1.interfaces.Configurable#setAttribute(java.lang.String, java.lang.String)
     */
    public void setAttribute(String key, String value) throws IllegalAttributeException {
        if(key.equals("main-class")){
            mainclass = value;
        }else if(key.equals("classpath")){
            classpath = value;
        }else if(key.equals("tunnel-class")){
            tunnelclass = value;
        }else if(key.equals("tunnel-type")){
            if(!value.equals("shell"))
                throw new IllegalAttributeException(module + " This launcher requires a shell tunnel.");
            ttype = value;
        }else if(key.equals("jdwp-port")){
            jvmport = value;
        }else if(key.equals("vm-parameters")){
            vmParameters = value;        
        }else if(key.equals("gid")){
            gid = value;
    	}else if(key.equals("global-agent-address")){
            globalAddress = value;
        }else if(key.equals("app-parameters")){
            appParameters = value;
        }else if(key.equals("tunnel-closure-policy")){
            if(value.equals("launch-and-close")){
                block = false;
            }else if(value.equals("wait-until-dead")){
                block = true;
            }else{
                throw new IllegalAttributeException(module + "Tunnel closure policy can be either launch-and-close or wait-until-dead");
            }
    	}else{
            if((tunnelclass == null) || !(key.startsWith(tunnelclass))){
                throw new IllegalAttributeException(module + " Unrecognized attribute "+ key +" (is tunnel-class unset?).");
            } else {
                ConversionTrait sh = ConversionTrait.getInstance();
                key = sh.extractPrefix(key, tunnelclass);
                tunnelAttributes.put(key, value);
            }
        }
        
    }

    /* (non-Javadoc)
     * @see ddproto1.interfaces.Configurable#getAttributeValue(java.lang.String)
     */
    public String getAttribute(String key) throws IllegalAttributeException {
        if(key.equals("main-class")){
            return mainclass;
        }else if(key.equals("classpath")){
            return classpath;
        }else if(key.equals("tunnel-class")){
            return tunnelclass;
        }else if(key.equals("tunnel-type")){
            return ttype;
        }else if(key.equals("jdwp-port")){
            return jvmport;
        }else if(key.equals("gid")){
            return gid;
    	}else if(key.equals("vm-parameters")){
            return vmParameters;
    	}else if(key.equals("global-agent-address")){
    	    return globalAddress;
    	}else{
            if((tunnelclass == null) || !(key.startsWith(tunnelclass))){
                throw new IllegalAttributeException(module + " Unrecognized attribute "+ key +" (is tunnel-class unset?).");
            } else {
                ConversionTrait sh = ConversionTrait.getInstance();
                key = sh.extractPrefix(key, tunnelclass);
                return (String)tunnelAttributes.get(key);
            }
        }

    }

    /* (non-Javadoc)
     * @see ddproto1.interfaces.Configurable#getAttributeKeys()
     */
    public String[] getAttributeKeys() {
        return attributes;
    }
    
    private void passParameters(IShellTunnel t)
    	throws IllegalAttributeException
    {
        Iterator it = tunnelAttributes.keySet().iterator();
        while(it.hasNext()){
            String key = (String)it.next();
            t.setAttribute(key, (String)tunnelAttributes.get(key));
        }
    }
    
    private void wrap(Exception e)
    	throws LauncherException
    {
        MessageHandler mh = MessageHandler.getInstance();
        mh.getErrorOutput().println(module + " Caught exception. Generating LauncherException");
        mh.printStackTrace(e);
        
        throw new LauncherException(e.toString());
    }

    public Set getAttributesByGroup(String prefix){
        throw new UnsupportedException(); 
    }

    public void addAttribute(String prefix){
        throw new UnsupportedException(); 
    }
    
    private class CommandAndClose implements Runnable{
        private IShellTunnel ish;
        private String cmd;
        
        private CommandAndClose(IShellTunnel sh, String command){
            this.ish = sh;
            this.cmd = command;
        }
        
        public void run() {
            try{
                ish.feedCommand(cmd);
                ish.close(false);
            }catch(Exception e){
                throw new NestedRuntimeException(e.getMessage(), e);
            }
        }
    }
}
