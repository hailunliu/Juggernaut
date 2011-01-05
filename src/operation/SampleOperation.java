package operation;

import core.Cache;
import core.Configuration;
import core.TaskManager;
import util.DateTools;
import util.SystemTools;
import launch.LaunchAgent;

import logger.ILogConfig.Module;
import data.AbstractOperation;

public class SampleOperation extends AbstractOperation {

	private SampleOperationConfig config;
	
	public SampleOperation(Configuration configuration, Cache cache, TaskManager taskManager, LaunchAgent parent, SampleOperationConfig config) {
		super(configuration, cache, taskManager, parent, config);
		this.config = config;
	}

	@Override
	public String getDescription() {
		return 
			"Idle: "+DateTools.millis2sec(config.getIdleTime())+" sec" +
			(config.isThrowError() ? " / Throwing: error" : "") +
			(config.isThrowException() ? " / Throwing: exception" : "") + 
			(config.isThrowError() && config.isThrowException() ? " / Throwing: error & exception" : "");
	}
	
	@Override
	protected void execute() throws Exception {
		
		if(config.isThrowError()){
			logger.log(Module.COMMON, "throwing error...");
			addError("Sample Error");
		}
		if(config.isThrowException()){
			logger.log(Module.COMMON, "throwing exception...");
			throw new Exception("Sample Exception");
		}
		
		long work = DateTools.millis2sec(config.getIdleTime());
		if(work > 0){
			logger.log(Module.COMMON, "doing some work...");
			for(long i=1; i<=work; i++){
				logger.debug(Module.COMMON, "work ("+i+"/"+work+")");
				SystemTools.sleep(DateTools.sec2millis(1));
			}
		}
		logger.log(Module.COMMON, "done.");
	}
}
