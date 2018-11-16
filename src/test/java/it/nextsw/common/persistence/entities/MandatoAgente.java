package it.nextsw.common.persistence.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "mandati_agente")
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property=EntityConstants.DEFAULT_ID_PROPERTY_NAME)
public class MandatoAgente extends PersistentObject {


    @Column(name="DATA_INIZIO", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date dataInizio;
    @Column(name="DATA_FINE", nullable = true)
    @Temporal(TemporalType.DATE)
    private Date dataFine;
    @Column(name="DESCRIZIONE", nullable = true, length= EntityConstants.VARCHAR_FIELD_LARGE)
    private String descrizione;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="AGENTE_"+EntityConstants.DEFAULT_ID_COLUMN_NAME)
    @JsonIgnore
    private Agente agente;


    @ManyToOne
    @JoinColumn(name="MERCATO_RIFERIMENTO_"+EntityConstants.DEFAULT_ID_COLUMN_NAME, nullable = true)
    private MercatoRiferimento mercatoRiferimento;


    public Date getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(Date dataInizio) {
        this.dataInizio = dataInizio;
    }

    public Date getDataFine() {
        return dataFine;
    }

    public void setDataFine(Date dataFine) {
        this.dataFine = dataFine;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public Agente getAgente() {
        return agente;
    }

    public void setAgente(Agente agente) {
        this.agente = agente;
    }

    public MercatoRiferimento getMercatoRiferimento() {
        return mercatoRiferimento;
    }

    public void setMercatoRiferimento(MercatoRiferimento mercatoRiferimento) {
        this.mercatoRiferimento = mercatoRiferimento;
    }
}
