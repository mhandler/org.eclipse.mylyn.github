package org.eclipse.mylyn.github.internal.model;

public class GitHubComment {

    private String created_at;

    private String body;

    private String updated_at;

    private String id;

    private String user;

    public GitHubComment() {
        this.created_at = "";
        this.body = "";
        this.updated_at = "";
        this.id = "";
        this.user = "";
    }

    public GitHubComment(String created_at, String body, String updated_at, String id, String user) {
        this.created_at = created_at;
        this.body = body;
        this.updated_at = updated_at;
        this.id = id;
        this.user = user;
    }

    public String getBody() {
        return body;
    }

    public String getCreated_at() {
        return created_at;
    }

    public String getId() {
        return id;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public String getUser() {
        return user;
    }

}
