package data;

import java.util.UUID;

import lifecycle.LaunchAgent;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

import data.Option.Type;



/**
 * the configuration of an operation,- will be serialized
 */
public abstract class AbstractOperationConfig {

	public enum OPTIONS {
		ACTIVE, CRITICAL
	}
	
	private String id;
	protected OptionContainer optionContainer;
	
	public AbstractOperationConfig(){
		
		id = UUID.randomUUID().toString();
		
		optionContainer = new OptionContainer();
		optionContainer.setDescription(getDescription());
		optionContainer.getOptions().add(new Option(
				OPTIONS.ACTIVE.toString(), "The operation's active state",
				Type.BOOLEAN, true
		));
		optionContainer.getOptions().add(new Option(
				OPTIONS.CRITICAL.toString(), "Errors will aboard the launch",
				Type.BOOLEAN, false
		));
	}
	
	public String getId(){ return id; }
	
	public OptionContainer getOptionContainer(){ return optionContainer; }
	
	public boolean isActive(){ 
		return optionContainer.getOption(OPTIONS.ACTIVE.toString()).getBooleanValue(); 
	}
	
	public boolean isCritical() {
		return optionContainer.getOption(OPTIONS.CRITICAL.toString()).getBooleanValue(); 
	}
	
	@Override
	public String toString(){ 
		if(isActive()){
			return (isValid() ? "" : "~ ") + getName();
		}else{
			return (isValid() ? "" : "~ ") + "<"+getName()+">";
		}
	}
	
	public abstract String getName();
	public abstract String getDescription();
	public abstract boolean isValid();
	public abstract AbstractOperation createOperation(LaunchAgent parent);
	
	public AbstractOperationConfig clone(){
		
		XStream xstream = new XStream(new DomDriver());
		String xml = xstream.toXML(this);
		AbstractOperationConfig config = (AbstractOperationConfig)xstream.fromXML(xml);
		return config;
	}
}