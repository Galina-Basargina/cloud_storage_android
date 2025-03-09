package ru.cloudstorage.client.rest.cloudstorage;

import java.util.Date;

public class File {
    private Integer id;
    private String original_filename;
    private String url_filename;
    private Integer filesize;
    private String content_type;
    private Date upload_date;

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getOriginalFilename() {
        return original_filename;
    }
    public void setOriginalFilename(String original_filename) {
        this.original_filename = original_filename;
    }
    public String getUrlFilename() {
        return url_filename;
    }
    public void setUrl_filename(String urlFilename) {
        this.url_filename = urlFilename;
    }
    public Integer getFilesize() {
        return filesize;
    }
    public void setFilesize(Integer filesize) {
        this.filesize = filesize;
    }
    public String getContentType() {
        return content_type;
    }
    public void setContentType(String content_type) {
        this.content_type = content_type;
    }
    public Date getUploadDate() {
        return upload_date;
    }
    public void setUploadDate(Date upload_date) {
        this.upload_date = upload_date;
    }
}
