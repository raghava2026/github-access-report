package com.github.accessreport.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Collaborator {

    private String login;
    private long id;

    @JsonProperty("role_name")
    private String roleName;
}