package com.mavensconsulting.renamer;

import java.util.Properties;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sforce.soap.metadata.CodeCoverageWarning;
import com.sforce.soap.metadata.DeployDetails;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.RunTestFailure;
import com.sforce.soap.metadata.RunTestsResult;

public class MessageHandler {
	
	private String WORK_FOLDER;
	private Properties appProps;
	private Logger logger;
	private Level defaultLoggingLevel;
	
	public MessageHandler (Properties p) throws IOException {
		appProps = p;
		WORK_FOLDER = appProps.getProperty("WORK_FOLDER");
		defaultLoggingLevel = Level.WARNING;
		
		logger = Logger.getLogger("RenameLog");
		FileHandler handler = new FileHandler(WORK_FOLDER + "/output.log",true);
		logger.addHandler(handler);
	}
	
	public void print(String message, Level level) {
		System.out.println(message);
		logger.log(level, message);
	}
	
	public void print(String message) {
		print(message,defaultLoggingLevel);
	}
	
	public void print(Exception e) {
		System.out.println(e.getMessage());
		logger.log(defaultLoggingLevel,e.getMessage(),e);
	}
	
	public void print(com.sforce.soap.metadata.Error e) {
		System.out.println("Status code: " + e.getStatusCode());
		System.out.println("Error message: " + e.getMessage());
		
		logger.log(defaultLoggingLevel, "Status code: " + e.getStatusCode());
		logger.log(defaultLoggingLevel, "Error message: " + e.getMessage());
	}
	
    /*
     * Print out any errors, if any, related to the deploy.
     * @param result - DeployResult
     */
     public void printErrors(DeployResult result, String messageHeader) {
         DeployDetails details = result.getDetails();
         StringBuilder stringBuilder = new StringBuilder();
         if (details != null) {
             DeployMessage[] componentFailures = details.getComponentFailures();
             for (DeployMessage failure : componentFailures) {
                 String loc = "(" + failure.getLineNumber() + ", " + failure.getColumnNumber();
                 if (loc.length() == 0 && !failure.getFileName().equals(failure.getFullName()))
                 {
                     loc = "(" + failure.getFullName() + ")";
                 }
                 stringBuilder.append(failure.getFileName() + loc + ":" 
                     + failure.getProblem()).append('\n');
             }
             RunTestsResult rtr = details.getRunTestResult();
             if (rtr.getFailures() != null) {
                 for (RunTestFailure failure : rtr.getFailures()) {
                     String n = (failure.getNamespace() == null ? "" :
                         (failure.getNamespace() + ".")) + failure.getName();
                     stringBuilder.append("Test failure, method: " + n + "." +
                             failure.getMethodName() + " -- " + failure.getMessage() + 
                             " stack " + failure.getStackTrace() + "\n\n");
                 }
             }
             if (rtr.getCodeCoverageWarnings() != null) {
                 for (CodeCoverageWarning ccw : rtr.getCodeCoverageWarnings()) {
                     stringBuilder.append("Code coverage issue");
                     if (ccw.getName() != null) {
                         String n = (ccw.getNamespace() == null ? "" :
                         (ccw.getNamespace() + ".")) + ccw.getName();
                         stringBuilder.append(", class: " + n);
                     }
                     stringBuilder.append(" -- " + ccw.getMessage() + "\n");
                 }
             }
         }
         if (stringBuilder.length() > 0) {
             stringBuilder.insert(0, messageHeader);
             this.print(stringBuilder.toString());
         }
     }
}
