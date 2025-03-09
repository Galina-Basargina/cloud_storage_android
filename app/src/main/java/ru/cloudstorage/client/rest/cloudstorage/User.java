package ru.cloudstorage.client.rest.cloudstorage;

public class User {
    private String login;
    private Integer root_folder;

    public String getLogin() {
        return login;
    }
    public void setLogin(String login) {
        this.login = login;
    }
    public Integer getRootFolder() {
        return root_folder;
    }
    public void setRootFolder(Integer rootFolder) {
        this.root_folder = rootFolder;
    }
}

