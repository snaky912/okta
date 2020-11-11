package com.serverless;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Command {
    @JsonProperty("type") 
    public String getType() { 
		 return this.type; 
	} 
    public void setType(String type) { 
		 this.type = type; 
	} 
    String type;
    @JsonProperty("value") 
    public Value getValue() { 
		 return this.value; 
	} 
    public void setValue(Value value) { 
		 this.value = value; 
	} 
    Value value;
}
