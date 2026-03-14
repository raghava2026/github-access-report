package com.github.accessreport.service;

import com.github.accessreport.client.GitHubClient;
import com.github.accessreport.model.AccessReport;
import com.github.accessreport.model.Collaborator;
import com.github.accessreport.model.Repository;
import com.github.accessreport.model.UserAccess;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service

@RequiredArgsConstructor
public class GitHubService {

    // ─── Replace @Slf4j with this line ──────────────────────────────────────
    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);

    private final GitHubClient gitHubClient;
    private final Executor taskExecutor;

    @Value("${github.org}")
    private String org;

    @Cacheable("accessReport")
    public AccessReport buildAccessReport() {

        log.info("Starting access report for org: {}", org);
        List<Repository> repos = gitHubClient.fetchRepositories(org);
        log.info("Found {} repositories", repos.size());

        List<CompletableFuture<Map.Entry<Repository, List<Collaborator>>>> futures =
            repos.stream()
                .map(repo -> CompletableFuture.supplyAsync(
                    () -> {
                        log.debug("Fetching collaborators for repo: {}", repo.getName());
                        List<Collaborator> collaborators =
                            gitHubClient.fetchCollaborators(org, repo.getName());
                        return Map.entry(repo, collaborators);
                    },
                    taskExecutor
                ))
                .collect(Collectors.toList());

        List<Map.Entry<Repository, List<Collaborator>>> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        log.info("All collaborator fetches complete");

        Map<String, List<UserAccess.RepoAccess>> userMap = new HashMap<>();

        for (Map.Entry<Repository, List<Collaborator>> entry : results) {
            Repository repo = entry.getKey();
            List<Collaborator> collaborators = entry.getValue();

            for (Collaborator collab : collaborators) {
                userMap
                    .computeIfAbsent(collab.getLogin(), k -> new ArrayList<>())
                    .add(UserAccess.RepoAccess.builder()
                        .name(repo.getName())
                        .fullName(repo.getFullName())
                        .privateRepo(repo.isPrivateRepo())
                        .role(collab.getRoleName())
                        .build());
            }
        }

        List<UserAccess> users = userMap.entrySet().stream()
                .map(e -> UserAccess.builder()
                    .login(e.getKey())
                    .repositories(e.getValue())
                    .totalRepos(e.getValue().size())
                    .build())
                .sorted(Comparator.comparingInt(UserAccess::getTotalRepos).reversed())
                .collect(Collectors.toList());

        log.info("Report built — {} users found", users.size());

        return AccessReport.builder()
                .organization(org)
                .generatedAt(Instant.now().toString())
                .totalRepositories(repos.size())
                .totalUsers(users.size())
                .users(users)
                .build();
    }
}