package ru.cloudstorage.client.rest.cloudstorage;

public class Folder {
    private Integer id;
    private Integer parent;
    private Integer owner;
    private String name;

    public Integer getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public Integer getParent() {
        return parent;
    }
    public void setParent(int parent) {
        this.parent = parent;
    }
    public Integer getOwner() {
        return owner;
    }
    public void setOwner(int owner) {
        this.owner = owner;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
