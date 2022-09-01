package it.nextsw.common.utils;

/**
 *
 * @author gdm
 */
public class ForeignKey {

    private Object id;
    private String targetEntity;
    private String url;

    public ForeignKey(Object id, String targetEntity, String url) {
        this.id = id;
        this.targetEntity = targetEntity;
        this.url = url;
    }

    public ForeignKey() {
    }

    public Object getId() {
        return id;
    }

    public void setId(Object id) {
        this.id = id;
    }

    public String getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(String targetEntity) {
        this.targetEntity = targetEntity;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
