package com.github.accessreport.client;

import com.github.accessreport.model.Collaborator;
import com.github.accessreport.model.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubClient {

    private final RestTemplate restTemplate;

    @Value("${github.api-base-url}")
    private String apiBaseUrl;

    @Value("${github.token}")
    private String token;

    @Value("${github.per-page:100}")
    private int perPage;

    // ─── Build auth headers for every request ───────────────────────────────
    private HttpEntity<Void> authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
        return new HttpEntity<>(headers);
    }

    // ─── Generic paginated fetcher ───────────────────────────────────────────
    private <T> List<T> fetchAllPages(String url, Class<T[]> responseType) {
        List<T> all = new ArrayList<>();
        int page = 1;

        while (true) {
            String pagedUrl = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                    .toUriString();

            log.debug("Fetching page {} from {}", page, pagedUrl);

            ResponseEntity<T[]> response = restTemplate.exchange(
                    pagedUrl,
                    HttpMethod.GET,
                    authHeaders(),
                    responseType
            );

            T[] body = response.getBody();
            if (body == null || body.length == 0) break;

            all.addAll(Arrays.asList(body));

            // if we got less than perPage items, this was the last page
            if (body.length < perPage) break;
            page++;
        }

        log.debug("Total items fetched from {}: {}", url, all.size());
        return all;
    }

    // ─── Fetch all repositories in the org ──────────────────────────────────
    public List<Repository> fetchRepositories(String org) {
        String url = apiBaseUrl + "/orgs/" + org + "/repos?type=all";
        log.info("Fetching repositories for org: {}", org);
        return fetchAllPages(url, Repository[].class);
    }

    // ─── Fetch all collaborators for one repo ───────────────────────────────
    public List<Collaborator> fetchCollaborators(String org, String repoName) {
        String url = apiBaseUrl + "/repos/" + org + "/" + repoName
                + "/collaborators?affiliation=all";
        try {
            return fetchAllPages(url, Collaborator[].class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("No access to collaborators for repo: {}", repoName);
                return Collections.emptyList();
            }
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("Repo not found: {}", repoName);
                return Collections.emptyList();
            }
            throw e;
        }
    }
}
