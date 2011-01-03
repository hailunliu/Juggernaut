package launch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import operation.IRepositoryOperation;

import core.Cache;
import core.Configuration;
import core.FileManager;
import core.History;
import core.TaskManager;
import data.AbstractOperation;
import data.AbstractOperationConfig;
import data.AbstractTrigger;
import data.Artifact;
import data.LaunchHistory;
import data.LaunchConfig;
import data.OperationHistory;
import smtp.SmtpClient;
import util.FileTools;
import util.StringTools;
import util.SystemTools;
import launch.StatusManager.Status;
import data.Error;

import logger.Logger;
import logger.Logger.Mode;
import logger.ILogConfig.Module;

public class LaunchAgent extends LifecycleObject {

	private History history;
	private FileManager fileManager;
	private LaunchConfig launchConfig;
	private String trigger;
	private PropertyContainer propertyContainer;
	private ArrayList<AbstractOperation> operations;
	private LaunchNotification launchNotification;
	private LaunchHistory launchHistory;
	private Logger logger;
	private boolean aboard;
	
	public LaunchAgent(
			Configuration configuration, 
			Cache cache,
			History history, 
			FileManager fileManager, 
			TaskManager taskManager, 
			SmtpClient smtpClient,
			LaunchConfig launchConfig,
			String trigger)
	{
		super("Launch("+launchConfig.getId()+")", taskManager);
		
		this.fileManager = fileManager;
		this.history = history;
		this.launchConfig = launchConfig.clone();
		this.trigger = trigger;
		logger = new Logger(Mode.FILE);
		logger.setConfig(configuration);
		
		propertyContainer = new PropertyContainer();
		propertyContainer.addProperty(launchConfig.getId(), "Name", launchConfig.getName());
		propertyContainer.addProperty(launchConfig.getId(), "Folder", getFolder());
		propertyContainer.addProperty(launchConfig.getId(), "Trigger", trigger);
		propertyContainer.addProperty(launchConfig.getId(), "Clean", ""+launchConfig.isClean());
		propertyContainer.addProperty(launchConfig.getId(), "Timeout", StringTools.millis2min(launchConfig.getTimeout())+" min");
		
		operations = new ArrayList<AbstractOperation>();
		for(AbstractOperationConfig operationConfig : launchConfig.getOperationConfigs()){
			if(operationConfig.isActive()){
				AbstractOperation operation = operationConfig.createOperation(configuration, cache, taskManager, this);
				operations.add(operation);
				propertyContainer.addProperties(
						operationConfig.getId(), 
						operationConfig.getOptionContainer().getProperties()
				);
			}
		}
		
		launchNotification = new LaunchNotification(smtpClient, cache, this);
		
		launchHistory = new LaunchHistory(this, fileManager);
		for(AbstractOperation operation : operations){
			launchHistory.operations.add(new OperationHistory(operation, fileManager));
		}
		
		statusManager.setProgressMax(operations.size());
		aboard = false;
	}
	
	public LaunchConfig getConfig(){ return launchConfig; }
	public String getTrigger(){ return trigger; }
	public PropertyContainer getPropertyContainer(){ return propertyContainer; }
	public ArrayList<AbstractOperation> getOperations(){ return operations; }
	
	@Override
	public String getFolder() {
		return 
			fileManager.getBuildFolderPath()+
			File.separator+launchConfig.getId();
	}

	@Override
	public Logger getLogger() { return logger; }
	
	@Override
	protected void init() throws Exception {
		
		// setup the history-folder
		launchHistory.init();
		
		// setup the logger
		logger.setLogFile(new File(launchHistory.logfile), 0);
		logger.info(Module.COMMON, "Launch ["+launchConfig.getName()+"]");
		artifacts.add(new Artifact("Logfile", logger.getLogFile()));
		debugProperties(propertyContainer.getProperties(launchConfig.getId()));
		
		// setup launch-folder
		File folder = new File(getFolder());
		if(launchConfig.isClean() && folder.isDirectory()){
			logger.log(Module.COMMON, "cleaning: "+folder.getAbsolutePath());
			FileTools.deleteFolder(folder.getAbsolutePath());
		}
		if(!folder.isDirectory()){
			FileTools.createFolder(folder.getAbsolutePath());
		}
	}
	
	@Override
	protected void execute() throws Exception {
		
		for(AbstractOperation operation : operations){
			try{
				Status operationStatus = executeOperation(operation);
				logger.log(Module.COMMON, "Operation: "+operationStatus.toString());	
				if(operationStatus == Status.ERROR){
					if(!operation.getConfig().isCritical()){
						statusManager.setStatus(Status.ERROR);
					}else{
						logger.emph(Module.COMMON, "Critical operation failed");
						statusManager.setStatus(Status.FAILURE);
						aboard = true;
					}
				}else if(operationStatus == Status.FAILURE){
					statusManager.setStatus(Status.FAILURE);
					aboard = true;
				}
			}catch(InterruptedException e){
				logger.emph(Module.COMMON, "Interrupted");
				statusManager.setStatus(Status.CANCEL);
				aboard = true;
				if(operation.isAlive()){
					operation.syncKill();
					operation.getStatusManager().setStatus(Status.CANCEL);
				}
			}finally{
				statusManager.addProgress(1);
			}
		}
	}

	private Status executeOperation(AbstractOperation operation)throws Exception {
		
		logger.info(
				Module.COMMON, 
				operation.getIndex()+"/"+launchConfig.getOperationConfigs().size()+
				" Operation ["+operation.getConfig().getName()+"]"
		);
		debugProperties(propertyContainer.getProperties(operation.getConfig().getId()));
		if(!aboard){
			operation.syncRun(0, 0);
			propertyContainer.addProperties(
					operation.getConfig().getId(), 
					operation.getStatusManager().getProperties()
			);
		}else{
			operation.getStatusManager().setStatus(Status.CANCEL);
		}
		return operation.getStatusManager().getStatus();
	}

	@Override
	protected void finish() {

		for(AbstractOperation operation : operations){
			if(StatusManager.isError(operation.getStatusManager().getStatus())){
				statusManager.addError(launchConfig.getId(), "Launch did not succeed");
				break;
			}
		}
		
		propertyContainer.addProperties(
				launchConfig.getId(), 
				statusManager.getProperties()
		);
		
		try{ 
			launchNotification.performNotification();
		}catch(Exception e){
			logger.error(Module.SMTP, e);
		}
		
		try{ 
			launchHistory.finish();
			history.add(launchHistory); 
		}catch(Exception e){
			logger.error(Module.COMMON, e);
		}
		
		if(trigger == AbstractTrigger.USER_TRIGGER){
			try{
				String path = launchHistory.getIndexPath();
				logger.debug(Module.COMMON, "browser: "+path);
				SystemTools.openBrowser(path);
			}catch(IOException e) {
				logger.error(Module.COMMON, e);
			}
		}
		
		logger.info(Module.COMMON, "Launch: "+statusManager.getStatus().toString());
		logger.clearListeners();
	}
	
	private void debugProperties(HashMap<String, String> properties) {
		
		StringBuilder info = new StringBuilder();
		ArrayList<String> keys = PropertyContainer.getKeys(properties);
		for(String key : keys){
			info.append("\t"+key+": "+properties.get(key)+"\n");
		}
		logger.debug(Module.COMMON, "Properties [\n"+info.toString()+"]");
	}
	
	public ArrayList<IRepositoryOperation> getRepositoryOperations(){
		
		ArrayList<IRepositoryOperation> list = new ArrayList<IRepositoryOperation>();
		for(AbstractOperation operation : operations){
			if(operation instanceof IRepositoryOperation){
				list.add((IRepositoryOperation) operation);
			}	
		}
		return list;
	}
	
	public ArrayList<Error> getNotifyingErrors(){
		
		ArrayList<Error> errors = new ArrayList<Error>();
		for(AbstractOperation operation : operations){
			if(operation.getConfig().isNotifying()){
				errors.addAll(operation.getStatusManager().getErrors());
			}
		}
		return errors;
	}
}
