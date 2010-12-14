package data;

import html.AbstractHtmlPage;
import html.HtmlLink;
import html.LaunchHistoryPage;
import html.OperationHistoryPage;

import java.io.File;


import core.Application;

public class OperationHistory extends AbstractHistory {
	
	private transient AbstractOperation operation;
	
	public OperationHistory(AbstractOperation operation){
		super(operation);
		this.operation = operation;
		
		id = operation.getConfig().getId();
		name = operation.getConfig().getName();
	}
	
	public void init() throws Exception {	
		
		folder = 
			Application.getInstance().getFileManager().getHistoryFolderPath()+
			File.separator+operation.getParent().getStatusManager().getStart().getTime()+
			File.separator+id;
		super.init();
	}
	
	public void finish() throws Exception {
		
		description = operation.getDescription();
		start = operation.getStatusManager().getStart();
		end = operation.getStatusManager().getEnd();
		status = operation.getStatusManager().getStatus();
		super.finish();
	}
	
	@Override
	protected AbstractHtmlPage getHtmlPage() {
		return new OperationHistoryPage(
				"Operation [ "+name+" ]", 
				getIndexPath(),
				new HtmlLink("&lt;&lt;", "../"+LaunchHistoryPage.OUTPUT_FILE),
				this
		);
	}
	
	public String getIndexPath() {
		return folder+File.separator+OperationHistoryPage.OUTPUT_FILE;
	}
}
