package util;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

public class UiTools {

	public static void infoDialog(String text){ 

		JOptionPane.showMessageDialog(null, text, "Info", JOptionPane.PLAIN_MESSAGE); 
	}
	
	public static void errorDialog(String text){ 

		JOptionPane.showMessageDialog(null, text, "Error", JOptionPane.ERROR_MESSAGE); 
	}

	public static String inputDialog(String text, String value){ 
		
		if(value != null){
			return JOptionPane.showInputDialog(text, value);
		}else{
			return JOptionPane.showInputDialog(text);
		}
	}	
	
	public static boolean confirmDialog(String text) {

		int option = UiTools.optionDialog(
				text, UiTools.YES_NO_OPTIONS
		);
		return option == UiTools.YES_OPTION;
	}

	public static final String[] YES_NO_OPTIONS = { "Yes", "No" };
	public static final String[] YES_NO_CANCEL_OPTIONS = { "Yes", "No", "Cancle" };
	
	public static final int INVALID_OPTION = -1;
	public static final int YES_OPTION = 0;
	public static final int NO_OPTION = 1;
	public static final int CANCEL_OPTION = 2;
	
	/**
	 * returns index of selected option or -1 if aboarded
	 */
	public static int optionDialog(String text, String[] options){ 

		return JOptionPane.showOptionDialog(
			null, text, "Confirm", 
			JOptionPane.YES_NO_CANCEL_OPTION, 
			JOptionPane.QUESTION_MESSAGE, null, options, options[0]
		);
	}	
	
	public static File folderDialog(String text, String path){		
		return filesystemDialog(text, path, JFileChooser.DIRECTORIES_ONLY);
	}
	
	public static File fileDialog(String text, String path){		
		return filesystemDialog(text, path, JFileChooser.FILES_ONLY);
	}
	
	private static File filesystemDialog(String text, String path, int mode){
		
		JFileChooser fileChooser = new JFileChooser(path);
		fileChooser.setDialogTitle(text);
		fileChooser.setFileSelectionMode(mode);
		fileChooser.setMultiSelectionEnabled(false);
		if(JFileChooser.APPROVE_OPTION == fileChooser.showOpenDialog(null)){
			return fileChooser.getSelectedFile();
		}else{
			return null;
		}
	}
}
