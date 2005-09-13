/*
 * Created on Jul 22, 2004
 *
 */
package ddproto1.launcher;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import ddproto1.configurator.newimpl.IConfigurationConstants;
import ddproto1.configurator.newimpl.IObjectSpec;
import ddproto1.configurator.newimpl.IServiceLocator;
import ddproto1.exception.AttributeAccessException;
import ddproto1.exception.CommException;
import ddproto1.exception.ConfigException;
import ddproto1.exception.IllegalAttributeException;
import ddproto1.exception.LauncherException;
import ddproto1.exception.NestedRuntimeException;
import ddproto1.exception.UnsupportedException;
import ddproto1.util.Lookup;
import ddproto1.util.MessageHandler;
import ddproto1.util.collection.ReadOnlyHashSet;
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
    private String ttype;
    private String vmParameters = "";
    private String gid;
    private String globalAddress;
    private String appParameters;
    private String cdwp_port;
    
    private Boolean block;
      
    private static final String module = "JVMShellLauncher -";
    
    private IShellTunnel sht;
    
    /* (non-Javadoc)
     * @see ddproto1.interfaces.ApplicationLauncher#launch()
     */
    public void launch() throws CommException, LauncherException, ConfigException {
        try{
            if ((classpath == null) || (mainclass == null) || (jvmport == null)
                    || (gid == null) || (appParameters == null)
                    || (block == null) || (globalAddress == null) || (cdwp_port == null))
                throw new ConfigException(module + " Required launcher parameter is missing.");
            
            IServiceLocator locator = (IServiceLocator) Lookup
                    .serviceRegistry().locate("service locator");
            
            if(sht == null){
                IObjectSpec thisSpec = locator.getMetaobject(this);
                IObjectSpec shtSpec = thisSpec.getChildSupporting(IShellTunnel.class);
                sht = (IShellTunnel)locator.incarnate(shtSpec);
            }
            
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
                    + " -Dagent.global.address=" + globalAddress + ":" + cdwp_port
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
    
    public boolean isWritable(){
        return true;
    }

    /* (non-Javadoc)
     * @see ddproto1.interfaces.Configurable#setAttribute(java.lang.String, java.lang.String)
     */
    public void setAttribute(String key, String value) throws IllegalAttributeException {
        if(key.equals("main-class")){
            mainclass = value;
        }else if(key.equals("classpath")){
            classpath = value;
        }else if(key.equals("tunnel-type")){
            if(!value.equals("shell"))
                throw new IllegalAttributeException(module + " This launcher requires a shell tunnel.");
            ttype = value;
        }else if(key.equals("jdwp-port")){
            jvmport = value;
        }else if(key.equals("vm-parameters")){
            vmParameters = value;        
        }else if(key.equals("guid")){
            gid = value;
    	}else if(key.equals("global-agent-address")){
            globalAddress = value;
        }else if(key.equals("cdwp-port")){
            cdwp_port = value;
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
    	    throw new IllegalAttributeException(module + " Unrecognized attribute "+ key +" (is tunnel-class unset?).");
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
    
    public IShellTunnel getShellTunnel(){
        if(sht == null)
            throw new IllegalStateException("Cannot acquire shell tunnel before launch");
        return sht;
    }
}
