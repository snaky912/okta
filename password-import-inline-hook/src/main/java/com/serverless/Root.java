package com.serverless;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Root {
    @JsonProperty("commands") 
    public List<Command> getCommands() {
		 return this.commands;
	}
    public void setCommands(List<Command> commands) {
		 this.commands = commands;
	}
    List<Command> commands;
}
