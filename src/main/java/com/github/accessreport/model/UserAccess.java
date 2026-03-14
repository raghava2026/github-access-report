package com.github.accessreport.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserAccess {

    private String login;
    private int totalRepos;
    private List<RepoAccess> repositories;

    @Data
    @Builder
    public static class RepoAccess {
        private String name;
        private String fullName;
        private boolean privateRepo;
        private String role;
    }
}