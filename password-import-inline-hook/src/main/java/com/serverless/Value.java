package com.serverless;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Value {
    @JsonProperty("credential") 
    public String getCredential() { 
		 return this.credential; 
	} 
    public void setCredential(String credential) { 
		 this.credential = credential; 
	} 
    String credential;
}