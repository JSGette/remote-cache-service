package com.gette;

class Write {
    private String resourceName;
    private boolean completed;
    private int commitedSize;

    public Write(String resource) {
        resourceName = resource;
    }

    public int getCommitedSize() {
        return commitedSize;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setCommitedSize(int commitedSize) {
        this.commitedSize = commitedSize;
    }
}
