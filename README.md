Salesforce-Renamer
==================

Salesforce-Renamer is a utility for mass renaming custom Salesforce fields and objects.  The tool is designed to assist a developer in changing the API name of fields or objects that may be referenced in code multiple times.

How It Works
------------

Here is the basic process that the renamer uses.

1. Download all code in the org
2. Clear/Delete all code from the org
3. Rename the fields and objects
4. Do a find and replace in the code to update the names
5. Put the code back

Getting Started
===============

* **WARNING:** Backup your Salesforce instance before running the utility, it modifies all code in the org
* **WARNING:** Read Known Issues before starting

Salesforce Renamer is a command line program and must be run from a command line.  Each step must be run in order for the rename to work.

To run the program you must have Java 7 installed.

1. Download SalesforceRenamer.jar and settings.properties and make sure they are in the same directory.
2. Edit settings.properties to add your credentials and desired paths
3. Open a command prompt in that directory.
4. Use java command to run the program.
    
    >java -jar SalesforceRenamer.jar


1) Preparation
--------------

1. Before renaming, the following steps must be done manually in the org:
+ Delete all scheduled jobs
+ Remove all Apex Sharing Recalculations from object settings
+ Remove any VisualForce overrides of standard Salesforce buttons
2. Fill out the settings file.
	+ Specify a working file location
	+ Specify the rename rule file, default is rename.csv
	+ Specify the Salesforce login URL to use
	+ (Optional) Specify Salesforce credentials.  If you don't put them in the settings files, you will be prompted for them when the application starts.
3. Create the rename rules.  A template with all available fields and objects to rename can be automaticaly generated by running the renamer and selecting option 0.

2) Do the renames
---------------------------------

1. Run the tool options 1-3 in order.
2. Rename any custom setting objects and Apex Sharing Reason names manually.
3. Run tool option #4
 * If #4 failes, it can be run again after fixing any issues in the code by editing the files in renamed.zip or in the org.

3) Post Steps
-------------
+ Re-schedule deleted scheduled jobs
+ Add back Apex Sharing reasons
+ Add back Visualforce overrides


KNOWN ISSUES
============
+ Rename does not happen automatically for Custom Setting Object Names
+ Fields with the same API name as other fields or objects cause issues.  The renamer does a simple find and replace so it cannot distinguish between the between fields from different objects so will rename all of them in the code.
+ Relationship names may not be renamed properly or as expected.
+ Org namespace must be null.  Meaning, it does not work properly on orgs where managed packages are being built.  


