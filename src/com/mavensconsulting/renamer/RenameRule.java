package com.mavensconsulting.renamer;

import java.util.HashMap;

//TODO figure out what to do about parent
public class RenameRule {
	private String oldName = "__c";
	private String newName = "__c";
	private String parent;
	private RuleType type;
	private HashMap<String,String> nameVariations;
	
	public RenameRule () {
		nameVariations = new HashMap<String,String>();
	}
	
	public String getOldName() {
		return oldName;
	}
	
	public String getOldFullName() {
		return parent + "." + oldName;
	}
	
	public void setOldName(String oldName) {
		this.oldName = oldName;
		buildNameVariations();
	}
	
	public String getNewName() {
		return newName;
	}
	
	public String getNewFullName() {
		return parent + "." + newName;
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
		return oldName.substring(0,oldName.length()-3);
	}
	
	public String getNewBaseName () {
		return newName.substring(0,newName.length()-3);
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
		buildNameVariations();
	}
	
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
			if(type == RuleType.CustomObject) {
				String newBase = getBase(newName);
				String oldBase = getBase(oldName);
				String[] postfixes = {"__Share","__History","__r","__Feed"};
				for(String variant : postfixes) {
					nameVariations.put(oldBase+variant,newBase+variant);
				}
			}
		}
	}
	
	private String getBase(String full) {
		String base = full.substring(0, full.length() - 3);
		return base;
	}
}
