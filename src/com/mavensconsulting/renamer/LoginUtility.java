package com.mavensconsulting.renamer;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import com.sforce.soap.partner.LoginResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Login utility.
 */
public class LoginUtility {
	public String API_VERSION = "31.0";
	public MetadataConnection metadataConnection;
	public PartnerConnection partnerConnection;
	
	private String USERNAME;
	private String PASSWORD;
	private String URL;
	
	private BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

	public LoginUtility (Properties appProps) {
		API_VERSION = appProps.getProperty("API_VERSION");
		USERNAME 	= appProps.getProperty("USERNAME");
		PASSWORD 	= appProps.getProperty("PASSWORD");
		URL 		= appProps.getProperty("URL") + "/services/Soap/u/" + API_VERSION;
		
		if(USERNAME.isEmpty()) {
			while(USERNAME.isEmpty()) {
				System.out.print("Enter Username: ");
		        String choice = null;
				try {
					choice = reader.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
		        USERNAME = choice != null ? choice.trim() : "";
			}
		}
		if(PASSWORD.isEmpty()) {
			while(PASSWORD.isEmpty()) {
				System.out.print("Enter Password: ");
		        String choice = null;
				try {
					choice = reader.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
				PASSWORD = choice != null ? choice.trim() : "";
			}
		}
	}
	
    public MetadataConnection login() throws ConnectionException {
    	ConnectorConfig partnerConfig = new ConnectorConfig();
    	partnerConfig.setAuthEndpoint(URL);
    	partnerConfig.setServiceEndpoint(URL);
    	partnerConfig.setUsername(USERNAME);
    	partnerConfig.setPassword(PASSWORD);
        partnerConnection = com.sforce.soap.partner.Connector.newConnection(partnerConfig);
        LoginResult lr = partnerConnection.login(USERNAME,PASSWORD);
    	
        ConnectorConfig metadataConfig = new ConnectorConfig();
        metadataConfig.setServiceEndpoint(lr.getMetadataServerUrl());
        metadataConfig.setSessionId(lr.getSessionId());
        metadataConnection = com.sforce.soap.metadata.Connector.newConnection(metadataConfig);
        return metadataConnection;
    }
}