package com.mavensconsulting.renamer;
import com.sforce.soap.metadata.*;
import com.sforce.ws.ConnectionException;

import java.util.*;
import java.io.*;

import org.apache.commons.csv.*;

/**
 * @author Kai Amundsen
 * 
 */
public class SalesforceRenamer {
	private Properties appProps;
	private LoginUtility sforce;
	private MessageHandler logger;
	private PackageHandler packager;
	private RenameUtility renamer;
	private ArrayList<RenameRule> renameList;
	private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	private String ZIP_FOLDER = ".";
    private String BACKUP_FILE = "backup.zip";
    private String DESTRUCT_FILE = "destruct.zip";
    private String BLANK_FILE = "emptyClasses.zip";
    private String RENAMED_FILE = "renamed.zip";
    private String RENAME_TEMPLATE = "rules_template.csv";
    private String RENAME_LIST = "rename.csv";
    private String API_VERSION = "31.0";
    CSVFormat format = CSVFormat.EXCEL.withHeader("Type","Parent","Old Name", "New Name");
	

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		SalesforceRenamer renamer = new SalesforceRenamer();
		renamer.runRename();
	}

	private void runRename() {
		Properties defaultProps = new Properties();
		defaultProps.setProperty("WORK_FOLDER", ".");
		defaultProps.setProperty("RENAME_LIST","rename.csv");
		defaultProps.setProperty("POLL_TIME", "1000");
		defaultProps.setProperty("MAX_NUM_POLL_REQUESTS", "50");
		defaultProps.setProperty("API_VERSION", "31.0");
		defaultProps.setProperty("URL", "https://test.salesforce.com/");
		appProps = new Properties(defaultProps);
		FileInputStream in = null;
		try {
			in = new FileInputStream("settings.properties");
			appProps.load(in);
		} catch (IOException eio) {
			System.out.println("Could not read in settings from file settings.properties");
			System.out.println(eio.getMessage());
		} finally {
			try{
				in.close();
			} catch (Exception e) {}
		}
		
		ZIP_FOLDER 		= appProps.getProperty("WORK_FOLDER",".");
		BACKUP_FILE 	= ZIP_FOLDER + "/backup.zip";
		DESTRUCT_FILE 	= ZIP_FOLDER + "/destruct.zip";
		BLANK_FILE 		= ZIP_FOLDER + "/emptyCode.zip";
		RENAMED_FILE 	= ZIP_FOLDER + "/renamed.zip";
		RENAME_TEMPLATE = ZIP_FOLDER + "/rules_template.csv";
		RENAME_LIST		= ZIP_FOLDER + appProps.getProperty("RENAME_LIST","rename.csv");
		API_VERSION 	= appProps.getProperty("API_VERSION","31.0");
		
			try {
			logger = new MessageHandler(appProps);
			sforce = new LoginUtility(appProps);
			packager = new PackageHandler(appProps, logger, sforce);
			renamer = new RenameUtility(appProps, logger, sforce);
			try {
				sforce.login();
				renameList = new ArrayList<RenameRule>();
	
		        String choice = getUsersChoice();
		        while (!choice.equalsIgnoreCase("q")) {
		            switch (choice) {
		            	case "0":
		            		// 0: Generate Rename Template
		            		createEmptyRename();
		            		break;
		            	case "1":
		            		// 1: Pull Information and Prepare for Rename
		            		String[] renameTypes = {"ApexClass","ApexComponent","ApexPage","ApexTrigger","CustomObject","EmailTemplate"};
			                packager.retrieveZip(renameTypes, BACKUP_FILE);
		            		renamer.createClearPackage(BACKUP_FILE, BLANK_FILE, DESTRUCT_FILE);
		            		break;
		            	case "2":
		            		// 2: Clear Code
		            		packager.deployZip(BLANK_FILE);
		            		packager.deployZip(DESTRUCT_FILE);
		            		break;
		            	case "3":
		            		// 3: Rename
		            		if(renameList.size() == 0) {
		            			loadRenameRules();
		            		}
		            		renamer.editAllFiles(BACKUP_FILE, RENAMED_FILE, renameList);
		            		renamer.renameAll(renameList);
		            	    System.out.println(" - Rename Custom Settings Objects manually now.");
		            		break;
		            	case "4":
		            		// 4: Deploy Renamed Code
		            		packager.deployZip(RENAMED_FILE);
		            		break;
		            	case "5":
		            		// 5: Restore Code From Backup
		            		if(renameList.size() == 0) {
		            			loadRenameRules();
		            		}
		            		ArrayList<RenameRule> undoes = new ArrayList<RenameRule>();
		            		for(RenameRule original : renameList) {
		            			RenameRule undo = new RenameRule();
		            			undo.setNewName(original.getNewName());
		            			undo.setOldName(original.getOldName());
		            			undo.setParent(original.getParent());
		            			undo.setType(original.getType());
		            			undoes.add(undo);
		            		}
		            		renamer.renameAll(undoes);
		            		break;
		            	default:
		            		System.out.println("Not a valid choice, please try again.");
		            		break;
		            }
		            // show the options again
		            choice = getUsersChoice();
		        }
			} catch (ConnectionException e) {
				logger.print(e);
			}
		} catch (Exception e){
			System.out.println(e.getMessage());
		}
	}
	
    /*
     * Utility method to present options to retrieve or deploy.
     */
    private String getUsersChoice() {
    	System.out.println("*NOTE*\n Before renaming take the following actions: ");
    	System.out.println(" - Delete all scheduled jobs");
    	System.out.println(" - Remove all Apex Sharing Recalculations from object settings");
    	System.out.println(" - Remove any VisualForce overrides of standard Salesforce buttons");
    	System.out.println(" - Change Apex Sharing Reason Names manually");
    	System.out.println();
    	System.out.println(" 0: Generate Rename Template");
        System.out.println(" 1: Pull Information and Prepare for Rename");
        System.out.println(" 2: Clear Code");
        System.out.println(" 3: Rename");
        System.out.println(" 4: Deploy Renamed Code");
        System.out.println(" 5: Restore Code From Backup");
        System.out.println(" q: Exit");
        System.out.println();
        System.out.print("Enter choice or q to exit: ");
        // wait for the user input.
        String choice = null;
		try {
			choice = reader.readLine();
		} catch (IOException e) {
			logger.print(e);
		}
        return choice != null ? choice.trim() : "";
    }
    
    private void loadRenameRules() {
    	renameList = new ArrayList<RenameRule>();
    	try {
    		CSVParser parser = CSVParser.parse(RENAME_LIST,format);
    		for (CSVRecord csvRecord : parser) {
    		     RenameRule rule = new RenameRule();
    		     rule.setType(RuleType.valueOf(csvRecord.get("Type")));
    		     rule.setParent(csvRecord.get("Parent"));
    		     rule.setOldName(csvRecord.get("Old Name"));
    		     rule.setNewName(csvRecord.get("New Name"));
    		}
    	} catch (IOException eio) {
    		logger.print(eio);
    	}
    	
    }
    
    private void createEmptyRename() {
    	ArrayList<String[]> existingTypes = new ArrayList<String[]>();
    	for(String object : getAvailableMetadata("CustomObject")) {
    		String[] objectLine = new String[4];
    		objectLine[0] = "CustomObject";
    		objectLine[1] = "";
    		objectLine[2] = object;
    		objectLine[3] = "";
    		existingTypes.add(objectLine);
    	}
    	
    	for(String object : getAvailableMetadata("CustomField")) {
    		String[] objectLine = new String[4];
    		String[] fieldSplit = object.split("\\.");
    		objectLine[0] = "CustomField";
    		objectLine[1] = fieldSplit[0];
    		objectLine[2] = fieldSplit[1];
    		objectLine[3] = "";
    		existingTypes.add(objectLine);
    	}
    	
    	logger.print("Writing rename template to file " + RENAME_TEMPLATE);
        File renameFile = new File(RENAME_TEMPLATE);
        FileWriter os =  null;
        CSVPrinter printer = null;
        try {
        	os = new FileWriter(renameFile);
        	printer = new CSVPrinter(os,format);
        	printer.printRecords(existingTypes);
        } catch (IOException eio) {
        	logger.print(eio);
        } finally {
        	try {
        		printer.close();
        		os.close();
        	} catch (IOException ioe) {
        		logger.print(ioe);
        	} 
        }
    }
    
	private String[] getAvailableMetadata (String type) {
		ArrayList<String> availableTypes = new ArrayList<String>();
		ListMetadataQuery query = new ListMetadataQuery();
		query.setType(type);
		try {
			FileProperties[] objects = sforce.metadataConnection.listMetadata(
					new ListMetadataQuery[] { query },Double.valueOf(API_VERSION));
			for (FileProperties fp : objects) {
				String name = fp.getFullName();
				availableTypes.add(name);
			}
		} catch (Exception e) {
			logger.print(e);
		}
		
		return availableTypes.toArray(new String[availableTypes.size()]);
	}
}
