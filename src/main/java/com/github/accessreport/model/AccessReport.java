package com.github.accessreport.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AccessReport {

    private String organization;
    private String generatedAt;
    private int totalRepositories;
    private int totalUsers;
    private List<UserAccess> users;
}
