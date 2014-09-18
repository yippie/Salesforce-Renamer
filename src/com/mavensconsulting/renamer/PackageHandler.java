package com.mavensconsulting.renamer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.sforce.soap.metadata.*;

public class PackageHandler {
	private MetadataConnection metadataConnection;
	private MessageHandler logger;
	private String API_VERSION = "31.0";
    // one second in milliseconds
    private long POLL_TIME = 1000;
    // maximum number of attempts to deploy the zip file
    private int MAX_NUM_POLL_REQUESTS = 50;
	
	public PackageHandler(Properties p, MessageHandler mh, LoginUtility lu){
		metadataConnection = lu.metadataConnection;
		logger = mh;
		API_VERSION 	= p.getProperty("API_VERSION");
		POLL_TIME 		= Long.valueOf(p.getProperty("POLL_TIME"));
		MAX_NUM_POLL_REQUESTS = Integer.valueOf(p.getProperty("MAX_NUM_POLL_REQUESTS"));
	}
	
	public Boolean deployZip(String fileName) {
		Boolean success = true;
    	logger.print("Deploying Zip: " + fileName);
    	byte[] zipBytes = null;
        File zipFile = new File(fileName);
        if (!zipFile.exists() || !zipFile.isFile()) {
            logger.print("Cannot find the zip file for deploy() on path: "
                + zipFile.getAbsolutePath());
            success = false;
        } else {
        	FileInputStream fileInputStream = null;
	        try {
	        	fileInputStream = new FileInputStream(zipFile);
	            ByteArrayOutputStream bos = new ByteArrayOutputStream();
	            byte[] buffer = new byte[4096];
	            int bytesRead = 0;
	            while (-1 != (bytesRead = fileInputStream.read(buffer))) {
	                bos.write(buffer, 0, bytesRead);
	            }
	
	            zipBytes = bos.toByteArray();
	            fileInputStream.close();
	        } catch (Exception e) {
	            logger.print(e);
	        } finally {
	        	try {
	        		fileInputStream.close();
	        	} catch (IOException ioe) {
	        		logger.print(ioe);
	        	}
	        }
	        
	        DeployOptions deployOptions = new DeployOptions();
	        deployOptions.setPerformRetrieve(false);
	        deployOptions.setRollbackOnError(true);
	        AsyncResult asyncResult;
	        DeployResult result;
			try {
				asyncResult = metadataConnection.deploy(zipBytes, deployOptions);
				result = waitForDeployCompletion(asyncResult.getId());
				if (!result.isSuccess()) {
		            logger.printErrors(result, "Final list of failures:\n");
		            logger.print("The files were not successfully deployed");
		            success = false;
		        } else {
		        	logger.print("The file " + fileName + " was successfully deployed\n");
		        }
			} catch (Exception e) {
				logger.print(e);
			}
        }
        return success;
    }
	
	public Boolean retrieveZip(String[] renameTypes, String fileName) {
		Boolean success = true;
        RetrieveRequest retrieveRequest = new RetrieveRequest();
        // The version in package.xml overrides the version in RetrieveRequest
        retrieveRequest.setApiVersion(Double.valueOf(API_VERSION));
        com.sforce.soap.metadata.Package packageManifest = null;
        List<PackageTypeMembers> listPackageTypes = new ArrayList<PackageTypeMembers>();
        
        for(String type : renameTypes) {
	        PackageTypeMembers packageTypes = new PackageTypeMembers();
	        packageTypes.setName(type);
	        
	        //Can't rely on wildcards so list all metadata
	        logger.print("Starting type: " + type);
	        ArrayList<String> metadataEntry = new ArrayList<String>();
	        if(type.equals("EmailTemplate")) {
	        	ListMetadataQuery folderQuery = new ListMetadataQuery();
				folderQuery.setType("EmailFolder");
				FileProperties[] folders;
				try {
					folders = metadataConnection.listMetadata(
							new ListMetadataQuery[] { folderQuery },Double.valueOf(API_VERSION));
					for (FileProperties folder : folders) {
						ListMetadataQuery query = new ListMetadataQuery();
						query.setType(type);
						query.setFolder(folder.getFullName());
						FileProperties[] objects = metadataConnection.listMetadata(
								new ListMetadataQuery[] { query },Double.valueOf(API_VERSION));
						for (FileProperties fp : objects) {
							if(fp.getNamespacePrefix() == null) {
								String recordName = fp.getFullName();
								metadataEntry.add(recordName);
							}
						}
					}
				} catch (Exception e) {
					success = false;
					logger.print(e);
				}
			} else {
				ListMetadataQuery query = new ListMetadataQuery();
				query.setType(type);
				FileProperties[] objects;
				try {
					objects = metadataConnection.listMetadata(
							new ListMetadataQuery[] { query },Double.valueOf(API_VERSION));
					for (FileProperties fp : objects) {
						if(fp.getNamespacePrefix() == null) {
							String recordName = fp.getFullName();
							metadataEntry.add(recordName);
						}
					}
				} catch (Exception e) {
					success = false;
					logger.print(e);
				}
				
			}
			
	        packageTypes.setMembers(metadataEntry.toArray(new String[metadataEntry.size()]));
	        listPackageTypes.add(packageTypes);
        }
        
        packageManifest = new com.sforce.soap.metadata.Package();
        packageManifest.setNamespacePrefix(null);
        PackageTypeMembers[] packageTypesArray =
                new PackageTypeMembers[listPackageTypes.size()];
        packageManifest.setTypes(listPackageTypes.toArray(packageTypesArray));
        packageManifest.setVersion(API_VERSION + "");
        retrieveRequest.setUnpackaged(packageManifest);

        try {
	        AsyncResult asyncResult = metadataConnection.retrieve(retrieveRequest);
	        RetrieveResult result = waitForRetrieveCompletion(asyncResult);
	
	        if (result.getStatus() == RetrieveStatus.Failed) {
	        	logger.print(result.getErrorStatusCode() + " msg: " +
	                    result.getErrorMessage());
	        } else if (result.getStatus() == RetrieveStatus.Succeeded) {  
		        // Print out any warning messages
		        StringBuilder stringBuilder = new StringBuilder();
		        if (result.getMessages() != null) {
		            for (RetrieveMessage rm : result.getMessages()) {
		                stringBuilder.append(rm.getFileName() + " - " + rm.getProblem() + "\n");
		            }
		        }
		        if (stringBuilder.length() > 0) {
		            logger.print("Retrieve warnings:\n" + stringBuilder);
		        }
		
		        logger.print("Writing results to zip file " + fileName);
		        File resultsFile = new File(fileName);
		        FileOutputStream os =  null;
		        try {
		        	os = new FileOutputStream(resultsFile);
			        os.write(result.getZipFile()); 
		        } catch (IOException eio) {
		        	logger.print(eio);
		        	success = false;
		        } finally {
		        	try {
		        		os.close();
		        	} catch (IOException ioe) {
		        		logger.print(ioe);
		        		success = false;
		        	} 
		        }
	        }
        } catch (Exception e) {
        	logger.print(e);
        	success = false;
        }
        
        return success;
    }
	
	private DeployResult waitForDeployCompletion(String asyncResultId) throws Exception {
        int poll = 0;
        long waitTimeMilliSecs = POLL_TIME;
        DeployResult deployResult;
        boolean fetchDetails;
        do {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration

            waitTimeMilliSecs *= 2;
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new Exception(
                    "Request timed out. If this is a large set of metadata components, " +
                    "ensure that MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            // Fetch in-progress details once for every 3 polls
            fetchDetails = (poll % 3 == 0);

            deployResult = metadataConnection.checkDeployStatus(asyncResultId, fetchDetails);
            logger.print("Status is: " + deployResult.getStatus());
            if (!deployResult.isDone() && fetchDetails) {
                logger.printErrors(deployResult, "Failures for deployment in progress:\n");
            }
        }
        while (!deployResult.isDone());

        if (!deployResult.isSuccess() && deployResult.getErrorStatusCode() != null) {
            throw new Exception(deployResult.getErrorStatusCode() + " msg: " +
                    deployResult.getErrorMessage());
        }
        
        if (!fetchDetails) {
            // Get the final result with details if we didn't do it in the last attempt.
            deployResult = metadataConnection.checkDeployStatus(asyncResultId, true);
        }
        
        return deployResult;
    }

    private RetrieveResult waitForRetrieveCompletion(AsyncResult asyncResult) throws Exception {
    	// Wait for the retrieve to complete
        int poll = 0;
        long waitTimeMilliSecs = POLL_TIME;
        String asyncResultId = asyncResult.getId();
        RetrieveResult result = null;
        do {
            Thread.sleep(waitTimeMilliSecs);
            // Double the wait time for the next iteration
            waitTimeMilliSecs *= 2;
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new Exception("Request timed out.  If this is a large set " +
                "of metadata components, check that the time allowed " +
                "by MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            result = metadataConnection.checkRetrieveStatus(
                    asyncResultId);
            logger.print("Retrieve Status: " + result.getStatus());
        } while (!result.isDone());         

        return result;
    }
}
