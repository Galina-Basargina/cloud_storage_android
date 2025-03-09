package ru.cloudstorage.client.rest.cloudstorage;

import java.util.List;

public class Storage {
    private User user;
    private Folder current_folder;
    private List<Folder> folders;
    private List<File> files;

    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }
    public Folder getCurrentFolder() {
        return current_folder;
    }
    public void setCurrentFolder(Folder current_folder) {
        this.current_folder = current_folder;
    }
    public List<Folder> getFolders() {
        return folders;
    }
    public void setFolders(List<Folder> folders) {
        this.folders = folders;
    }
    public List<File> getFiles() {
        return files;
    }

    public void setFiles(List<File> files) {
        this.files = files;
    }
}
