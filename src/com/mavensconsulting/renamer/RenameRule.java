package com.mavensconsulting.renamer;

import java.util.ArrayList;
import java.util.HashMap;

//TODO figure out what to do about parent
public class RenameRule {
	private String oldName = "__c";
	private String newName = "__c";
	private String parent = "";
	private RuleType type = null ;
	private HashMap<String,String> nameVariations;
	
	public RenameRule () {
		nameVariations = new HashMap<String,String>();
	}
	
	public String getOldName() {
		return oldName;
	}
	
	public String getOldFullName() {
		String fullName = "";
		if(parent != null && !parent.isEmpty()) {
			fullName += parent + ".";
		}
		fullName += oldName;
		return fullName;
	}
	
	public void setOldName(String oldName) {
		this.oldName = oldName;
		buildNameVariations();
	}
	
	public String getNewName() {
		return newName;
	}
	
	public String getNewFullName() {
		String fullName = "";
		if(parent != null && !parent.isEmpty()) {
			fullName += parent + ".";
		}
		fullName += newName;
		return fullName;
	}
	
	public void setNewName(String newName) {
		this.newName = newName;
		buildNameVariations();
	}
	
	public RuleType getType() {
		return type;
	}
	
	public void setType(RuleType type) {
		this.type = type;
		buildNameVariations();
	}
	
	public String getOldBaseName () {
		return getBase(oldName);
	}
	
	public String getNewBaseName () {
		return getBase(newName);
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
		buildNameVariations();
	}
	
	//Old name is the key, new name is the value
	public HashMap<String,String> getNameVariations() {
		return nameVariations;
	}
	
	public Boolean isComplete() {
		Boolean complete = false;
		if(!oldName.isEmpty() && !newName.isEmpty() && type != null) {
			if(type == RuleType.CustomField) {
				complete = !parent.isEmpty();
			} else {
				complete = true;
			}
		}
		return complete;
	}

	private void buildNameVariations() {
		if(isComplete()){
			nameVariations = new HashMap<String,String>();
			ArrayList<String> postfixes = new ArrayList<String>();
			postfixes.add("__c");
			postfixes.add("__r");
			if(type == RuleType.CustomObject) {
				postfixes.add("__Share");
				postfixes.add("__History");
				postfixes.add("__Feed");
			}
			String newBase = getBase(newName);
			String oldBase = getBase(oldName);
			
			for(String variant : postfixes) {
				nameVariations.put(oldBase+variant,newBase+variant);
			}
		}
	}
	
	private String getBase(String full) {
		String base = full.substring(0, full.length() - 3);
		return base;
	}
}
