package data;

import java.util.ArrayList;
import java.util.UUID;

import lifecycle.LaunchAgent;
import lifecycle.LaunchManager.TriggerStatus;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import data.Option.Type;


/**
 * the configuration of a launch,- will be serialized
 */
public class LaunchConfig implements Comparable<LaunchConfig> {
	
	public enum OPTIONS {
		ACTIVE, DESCRIPTION, CLEAN, TIMEOUT, NOTIFY, ADMINISTRATORS, MESSAGE
	}
	
	private String id;
	private String name;
	private OptionContainer optionContainer;
	private ArrayList<AbstractOperationConfig> operations;
	private ArrayList<AbstractTriggerConfig> triggers;
	private transient boolean dirty;
	
	public LaunchConfig(String name){

		id = UUID.randomUUID().toString();
		this.name = name;
		
		optionContainer = new OptionContainer();
		optionContainer.setDescription("the configuration of the launch");
		optionContainer.getOptions().add(new Option(
				OPTIONS.ACTIVE.toString(), "The launch's active state",
				Type.BOOLEAN, false
		));
		optionContainer.getOptions().add(new Option(
				OPTIONS.DESCRIPTION.toString(), "The launch's description", 
				Type.TEXT, ""
		));
		optionContainer.getOptions().add(new Option(
				OPTIONS.CLEAN.toString(), "Clean launch-folder on start",
				Type.BOOLEAN, true
		));
		optionContainer.getOptions().add(new Option(
				OPTIONS.TIMEOUT.toString(), "Timeout in minutes (0 = no timeout)", 
				Type.INTEGER, 0
		));
		optionContainer.getOptions().add(new Option(
				OPTIONS.NOTIFY.toString(), "Enable mail notification for launch",
				Type.BOOLEAN, false
		));
		optionContainer.getOptions().add(new Option(
				OPTIONS.ADMINISTRATORS.toString(), "mail-list of launch admins (comma seperated)", 
				Type.TEXT, ""
		));
		optionContainer.getOptions().add(new Option(
				OPTIONS.MESSAGE.toString(), "Optional notification message", 
				Type.TEXTAREA, ""
		));
		
		operations = new ArrayList<AbstractOperationConfig>();
		triggers = new ArrayList<AbstractTriggerConfig>();
		dirty = true;
	}
	
	public String getId(){ return id; }
	
	public void setName(String name){ this.name = name; }
	public String getName(){ return name; }
	
	/** answers if configuration could be launched */
	public boolean isReady(){
		return isActive() && !isDirty() && isValid();
	}

	public boolean isActive(){ 
		return optionContainer.getOption(OPTIONS.ACTIVE.toString()).getBooleanValue(); 
	}
	
	public String getDescription(){ 
		return optionContainer.getOption(OPTIONS.DESCRIPTION.toString()).getStringValue(); 
	}
	
	public void setActive(boolean active){ 
		optionContainer.getOption(OPTIONS.ACTIVE.toString()).setBooleanValue(active); 
	}
	
	public boolean isClean() {
		return optionContainer.getOption(OPTIONS.CLEAN.toString()).getBooleanValue();
	}
	
	public void setDirty(boolean dirty){ this.dirty = dirty; }
	public boolean isDirty(){ return dirty; }
	
	public boolean isValid(){
		
		for(AbstractOperationConfig config : operations){
			if(config.isActive() && !config.isValid()){ return false; }
		}
		for(AbstractTriggerConfig config : triggers){
			if(config.isActive() && !config.isValid()){ return false; }
		}
		
		return
			optionContainer.getOption(OPTIONS.TIMEOUT.toString()).getIntegerValue() >= 0;
	}
	
	public OptionContainer getOptionContainer(){ return optionContainer; }
	public ArrayList<AbstractOperationConfig> getOperationConfigs(){ return operations; }
	public ArrayList<AbstractTriggerConfig> getTriggerConfigs(){ return triggers; }
	
	public LaunchAgent createLaunch(TriggerStatus trigger){
		return new LaunchAgent(this, trigger);
	}
	
	@Override
	public String toString(){ 
		if(isActive()){
			return (isValid() ? "" : "~ ") + name + (isDirty() ? " *" : "");
		}else{
			return (isValid() ? "" : "~ ") + "<"+name+">" + (isDirty() ? " *" : "");
		}
	}

	@Override
	public int compareTo(LaunchConfig o) {
		return name.compareTo(o.getName());
	}
	
	public LaunchConfig clone(){
		
		XStream xstream = new XStream(new DomDriver());
		String xml = xstream.toXML(this);
		LaunchConfig config = (LaunchConfig)xstream.fromXML(xml);
		return config;
	}
}