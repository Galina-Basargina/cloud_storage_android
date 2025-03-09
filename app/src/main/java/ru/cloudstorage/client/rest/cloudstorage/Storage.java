package ru.cloudstorage.client.rest.cloudstorage;

import java.util.ArrayList;

public class Storage {
    private User user;
    private Folder current_folder;
    private ArrayList<Folder> folders;
    private ArrayList<File> files;

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
    public ArrayList<Folder> getFolders() {
        return folders;
    }
    public void setFolders(ArrayList<Folder> folders) {
        this.folders = folders;
    }
    public ArrayList<File> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<File> files) {
        this.files = files;
    }
}
