package ru.cloudstorage.client.ui.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ru.cloudstorage.client.rest.cloudstorage.File;
import ru.cloudstorage.client.rest.cloudstorage.Folder;

public class StorageItems extends ArrayList<String> {
    final private Map<String, Folder> folders = new HashMap<>();
    final private Map<String, File> files = new HashMap<>();

    @Override
    public void clear() {
        super.clear();
        files.clear();
        folders.clear();
    }

    public void addFolder(Folder folder) {
        this.add("/" + folder.getName());
        folders.put(folder.getName(), folder);
    }
    public void addParentFolder(Folder folder) {
        this.add("/..");
        folders.put("..", folder);
    }
    public Folder getFolder(String name) {
        if (name.isEmpty()) return null;
        if (name.charAt(0) != '/') return null;
        return folders.get(name.substring(1));
    }
    public Folder getFolderByPosition(int position) {
        if (position < 0) return null;
        if (position >= this.size()) return null;
        String name = this.get(position);
        return getFolder(name);
    }

    public void addFile(File file) {
        this.add(" " + file.getOriginalFilename());
        files.put(file.getOriginalFilename(), file);
    }
    public File getFile(String name) {
        if (name.isEmpty()) return null;
        if (name.charAt(0) != ' ') return null;
        return files.get(name.substring(1));
    }
    public File getFileByPosition(int position) {
        if (position < 0) return null;
        if (position >= this.size()) return null;
        String name = this.get(position);
        return getFile(name);
    }
}
