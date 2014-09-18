package com.mavensconsulting.renamer;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class RenameUtility {
	
    private String API_VERSION;
	private LoginUtility sforce;
	private MessageHandler logger;
	
	public RenameUtility(Properties p, MessageHandler mh, LoginUtility lu) {
		API_VERSION = p.getProperty("API_VERSION");
		sforce = lu;
		logger = mh;
	}

	public Boolean renameAll(ArrayList<RenameRule> renames) {
		Boolean success = false;
		
		for(RenameRule rr : renames) {
			if(rr.getType() == RuleType.CustomField) {
				success = success || renameSingleObject(rr);
			}
		}
		
		//Objects must be done after fields due to the parent object reference in field names
		for(RenameRule rr : renames) {
			if(rr.getType() == RuleType.CustomObject) {
				success = success || renameSingleObject(rr);
			}
		}
		
		return success;
	}
	
	private boolean renameSingleObject(RenameRule rule) {
		boolean success = false;
		String oldName 	= rule.getOldFullName();
		String newName 	= rule.getNewFullName();
		RuleType type 	= rule.getType();
		
		if (type.equals("CustomObject") || type.equals("CustomField")) {
			//TODO figure out how to rename custom settings

			try {
				logger.print("Renaming: " + oldName + " to " + newName);
				com.sforce.soap.metadata.SaveResult result = sforce.metadataConnection.renameMetadata(type.toString(), oldName,
						newName);
				if (!result.isSuccess()) {
					success = true;
					logger.print("Errors were encountered while saving "
							+ result.getFullName());
					for (com.sforce.soap.metadata.Error e : result.getErrors()) {
						logger.print(e);
					}
				}
			} catch (Exception e) {
				logger.print(e);
			}
		}
		
		return success;
	}
	
    
    public Boolean createClearPackage(String sourceFile, String clearFile, String destructFile) {
    	Boolean success = true;
    	ArrayList<String> triggerNames = new ArrayList<String>();
    	logger.print("Creating Zip: " + clearFile);
	    	try {
	    	// Setup file to write to
	    	ZipOutputStream editedZip = new ZipOutputStream(new FileOutputStream(clearFile));
	    	//Start reading
	    	ZipFile original = new ZipFile(sourceFile);
	    	for(Enumeration<? extends ZipEntry> e = original.entries(); e.hasMoreElements();) {
	    		ZipEntry entry = (ZipEntry) e.nextElement();
	    		String newValue = "";
	    		String newEntryName = entry.getName();
	    		Boolean modified = false;
	
				//Need to isolate file name without extension
				String[] filePath = entry.getName().split("/");
				String className = filePath[filePath.length - 1];
				className = className.substring(0, className.indexOf("."));
				
				if(entry.getName().endsWith(".cls")) {
					newValue = "//Temporary blank class\n";
					newValue += "@isTest \n";
					newValue += "public with sharing class ";
					newValue += className;
					newValue += " {\n\n}";
					modified = true;
				} else if(entry.getName().endsWith(".trigger")) {
					newValue = "//Temporary blank trigger\n";
					// Find trigger first line and copy it to preserve object
					try {
			    		InputStream is = original.getInputStream(entry);
			    		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			    		String line = reader.readLine();
			    		Boolean foundFirstLine = false;
			    		while(line != null) {
			    			if(line.startsWith("trigger ") || foundFirstLine) {
			    				newValue += line;
			    				foundFirstLine = true;
			    				if(line.contains("{")) {
			    					line = null;
			    				} else {
			    					line = reader.readLine();
			    				}
			    			} else {
			    				line = reader.readLine();
			    			}
			    		}
						newValue += "\n\n}";
						modified = true;
					} catch (Exception ex) {
						logger.print(ex);
					}
					triggerNames.add(className);
	
				} else if(entry.getName().endsWith(".page")) {
					newValue = "<!--Temporary blank page-->";
					newValue += "<apex:page>\n</apex:page>";
					modified = true;
				} else if(entry.getName().endsWith(".component")) {
					newValue = "<!--Temporary blank page-->";
					newValue += "<apex:component>\n</apex:component>";
					modified = true;
				} else {
					modified = false;
				}
	    		 
	
	    		//Write results
	    		if(modified) {
		    		ZipEntry newEntry = new ZipEntry(newEntryName);
		    		editedZip.putNextEntry(newEntry);
		    		editedZip.write(newValue.getBytes(),0,newValue.getBytes().length);
	    		} else {
	    			editedZip.putNextEntry(entry);
	    			byte[] b = new byte[2048];
	    			int length;
	    			InputStream is = original.getInputStream(entry);
	    			while((length = is.read(b)) > 0) {
	    				editedZip.write(b,0,length);
	    			}
	    		}
	    		editedZip.closeEntry();
	    	}
	    	
	    	editedZip.close();
	    	original.close();
	    	success = buildDestructFile(destructFile,triggerNames);
	    } catch (IOException eio) {
	    	logger.print(eio);
	    	success = false;
	    }
    	
    	return success;
    }
    
    public Boolean editAllFiles(String sourceFile, String outputFile, ArrayList<RenameRule> renames) {
    	Boolean success = true;
    	logger.print("Creating Zip: " + outputFile);
    	
	    	try {
	    	// Setup file to write to
	    	ZipOutputStream editedZip = new ZipOutputStream(new FileOutputStream(outputFile));
	    	//Start reading
	    	ZipFile original = new ZipFile(sourceFile);
	    	for(Enumeration<? extends ZipEntry> e = original.entries(); e.hasMoreElements();) {
	    		ZipEntry entry = (ZipEntry) e.nextElement();
	    		String newValue = "";
	    		String newEntryName = entry.getName();
	    		Boolean modified = false;
	    		
	    		//**** Do manipulation here! *****
	    		//TODO actually parse files intelligently using APIs to only rename a the correct field
	    		//Modify existing
    			//Convert the input into a string
	    		InputStream is = original.getInputStream(entry);
	    		StringBuilder inputStringBuilder = new StringBuilder();
	    		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
	    		String line = reader.readLine();
	    		while(line != null) {
		    		inputStringBuilder.append(line);
	    			inputStringBuilder.append("\n");
	    			line = reader.readLine();
	    		}
	    		newValue = inputStringBuilder.toString();
	    		//Do the replacing
	    		for(RenameRule rename : renames) {
	    			String oldName = rename.getOldBaseName();
	    			String newName = rename.getNewBaseName();
	    			newValue = newValue.replaceAll("(?i)" + oldName, newName);
	    			newValue = newValue.replaceAll("(?<!MVN)</relationshipName>", "_MVN</relationshipName>");
	    			newEntryName = newEntryName.replaceAll("(?i)" + oldName, newName);
	    		}
	    		reader.close();
	    		
	    		modified = true;
	
	
	    		//Write results
	    		if(modified) {
		    		ZipEntry newEntry = new ZipEntry(newEntryName);
		    		editedZip.putNextEntry(newEntry);
		    		editedZip.write(newValue.getBytes(),0,newValue.getBytes().length);
	    		} else {
	    			editedZip.putNextEntry(entry);
	    			byte[] b = new byte[2048];
	    			int length;
	    			InputStream i = original.getInputStream(entry);
	    			while((length = i.read(b)) > 0) {
	    				editedZip.write(b,0,length);
	    			}
	    		}
	    		editedZip.closeEntry();
	    	}
	    	
	    	editedZip.close();
	    	original.close();
	    } catch (IOException eio) {
	    	logger.print(eio);
	    	success = false;
	    }
	    return success;
    }
    
    private Boolean buildDestructFile (String destFile, ArrayList<String> triggerNames) {
    	Boolean success = true;
     	logger.print("Building destructive changes\n\n");
     	try {
	     	// Setup file to write to
	     	ZipOutputStream destructZip = new ZipOutputStream(new FileOutputStream(destFile));
	     	
	     	String destructString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
	     			"<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n" +
	     			"<types>\n";
	     	for(String trigger : triggerNames) {
	     		destructString += "<members>" + trigger + "</members>\n";
	     	}
	     	destructString += "<name>ApexTrigger</name>\n" +
	     			"</types>\n" +
	     			"<version>" + API_VERSION + "</version>\n" +
	     			"</Package>";
	     	ZipEntry destructEntry = new ZipEntry("unpackaged/destructiveChanges.xml");
	     	destructZip.putNextEntry(destructEntry);
	     	destructZip.write(destructString.getBytes(),0,destructString.getBytes().length);
	     	destructZip.closeEntry();
	     	
	     	String packageString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
	     			"<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n" +
	     			"<version>" + API_VERSION + "</version>\n" +
	     			"</Package>";
	     	ZipEntry packageEntry = new ZipEntry("unpackaged/package.xml");
	     	destructZip.putNextEntry(packageEntry);
	     	destructZip.write(packageString.getBytes(),0,packageString.getBytes().length);
	     	destructZip.closeEntry();
	     	
	     	destructZip.close();
     	} catch (IOException eio) {
     		logger.print(eio);
     		success = false;
     	}
     	
     	return success;
    }
}
