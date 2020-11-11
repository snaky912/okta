package com.serverless;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Value {
    String credential;
    String token;

    @JsonProperty("credential") 
    public String getCredential() { 
		 return this.credential; 
	} 
    public void setCredential(String credential) { 
		 this.credential = credential; 
	} 

    @JsonProperty("token") 
    public String getToken() { 
		 return this.token; 
	} 
    public void setToken(String token) { 
		 this.token = token; 
	} 
}