package it.nextsw.common.persistence.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
@Table(name = "aree_operative")
public class AreaOperativa extends PersistentObject {

    @Column(name = "AREA_NIELSEN", nullable = false, length= EntityConstants.VARCHAR_FIELD_MEDIUM)
    private String areaNielsen;
    @Column(name = "REGIONE", nullable = true, length= EntityConstants.VARCHAR_FIELD_MEDIUM)
    private String regione;
    @Column(name = "PROVINCIA", nullable = true, length= EntityConstants.VARCHAR_FIELD_MEDIUM)
    private String provincia;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinTable(name="agente_aree_operative",
            joinColumns={@JoinColumn(name="AREA_OPERATIVA_"+EntityConstants.DEFAULT_ID_COLUMN_NAME, referencedColumnName=EntityConstants.DEFAULT_ID_COLUMN_NAME, insertable=false,updatable=false)},
            inverseJoinColumns={@JoinColumn(name="AGENTE_"+EntityConstants.DEFAULT_ID_COLUMN_NAME, referencedColumnName=EntityConstants.DEFAULT_ID_COLUMN_NAME, insertable=false,updatable=false)})
    @JsonIgnore
    private Agente agente;

    public String getAreaNielsen() {
        return areaNielsen;
    }

    public void setAreaNielsen(String areaNielsen) {
        this.areaNielsen = areaNielsen;
    }



    public String getRegione() {
        return regione;
    }

    public void setRegione(String regione) {
        this.regione = regione;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public Agente getAgente() {
        return agente;
    }

    public void setAgente(Agente agente) {
        this.agente = agente;
    }
}
