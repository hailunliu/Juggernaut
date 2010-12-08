package util;

import java.util.Date;

import core.Application;

public abstract class Task extends Thread {

	protected Logger observer;
	private long delay;
	private long cycle;
	private long timeout;
	private Date start;

	public Task(String name, Logger logger){
		
		setName(name);
		observer = logger;
		delay = 0;
		cycle = 0;
		timeout = 0;
		start = null;
	}
	
	public Logger getObserver(){ return observer; }
	public void setCycle(long cycle){ this.cycle = cycle; }
	public boolean isExpired(){
		
		if(start != null && timeout > 0){
			if((new Date()).getTime() >= start.getTime()+timeout){
				return true;
			}else{
				return false;
			}
		}else{
			return false;
		}
	}
	
	public void run(){
		
		observer.debug("Starting Task ["+getName()+"]");
		try{
			Thread.sleep(delay);
			if(isCyclic()){
				runCyclicTask();
			}else{
				runSingleTask();
			}
		}catch(InterruptedException e){ 
			observer.debug("Interrupting Task ["+getName()+"]");
		}finally{
			observer.debug("Stopping Task ["+getName()+"]");
		}
	}
	
	private boolean isCyclic(){ return cycle > 0; }

	private void runCyclicTask() throws InterruptedException {
		
		while(isCyclic() && !isInterrupted()){
			runSingleTask();
			Thread.sleep(cycle);
		}
	}
	
	private void runSingleTask(){
		
		start = new Date();
		Application.getInstance().getTimeoutManager().register(this);
		try{
			runTask();
		}finally{
			Application.getInstance().getTimeoutManager().deregister(this);
			start = null;
		}
	}
	
	protected abstract void runTask();
	
	public void syncRun(long delay, long timeout) throws InterruptedException {
		
		asyncRun(delay, timeout);
		join();
	}
	
	public void asyncRun(long delay, long timeout){
		
		this.delay = delay;
		this.timeout = timeout;
		start();
	}
	
	public void syncKill(){
		
		asyncKill();
		while(isAlive()){ 
			SystemTools.sleep(50);
		}
	}
	
	public void asyncKill(){
		
		if(isAlive()){
			interrupt();
		}
	}
}
