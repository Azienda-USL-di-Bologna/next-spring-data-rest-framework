package it.nextsw.common.persistence.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
@Table(name = "contatti_rubrica",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"EMAIL","AGENTE_"+ EntityConstants.DEFAULT_ID_COLUMN_NAME})})
public class ContattoRubrica extends PersistentObject {

    @Column(name ="NOME", nullable = true, length= EntityConstants.VARCHAR_FIELD_MEDIUM)
    private String nome;

    @Column(name ="COGNOME", nullable = true, length= EntityConstants.VARCHAR_FIELD_MEDIUM)
    private String cognome;

    @Column(name ="TELEFONO", nullable = true, length= EntityConstants.VARCHAR_FIELD_MEDIUM)
    private String telefono;

    @Column(name ="EMAIL", nullable = false, length= EntityConstants.VARCHAR_FIELD_MEDIUM)
    private String email;

    @Column(name ="EMAIL_INVITO_INVIATA", nullable = false)
    private boolean emailInvitoInviata ;

    @Column(name ="INDIRIZZO", nullable = true, length= EntityConstants.VARCHAR_FIELD_LARGE)
    private String indirizzo;
    @Column(name ="CITTA", nullable = true, length= EntityConstants.VARCHAR_FIELD_MEDIUM)
    private String citta;
    @Column(name ="CAP", nullable = true, length= EntityConstants.VARCHAR_FIELD_MEDIUM)
    private String cap;
    @Column(name ="PROVINCIA", nullable = true, length= EntityConstants.VARCHAR_FIELD_MEDIUM)
    private String provincia;
    @Column(name ="NOTE", nullable = true, length= EntityConstants.VARCHAR_FIELD_DOUBLE_LARGE)
    private String note;

    /**
     * Serve per avere la relazione inversa
     * identifica l'agente a cui appartiene questo contatto
     */
    @ManyToOne(fetch= FetchType.LAZY, optional = false)
    @JoinColumn(name="AGENTE_"+ EntityConstants.DEFAULT_ID_COLUMN_NAME)
    @JsonIgnore
    private Agente agente;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public String getCitta() {
        return citta;
    }

    public void setCitta(String citta) {
        this.citta = citta;
    }

    public String getCap() {
        return cap;
    }

    public void setCap(String cap) {
        this.cap = cap;
    }

    public String getProvincia() {
        return provincia;
    }

    public void setProvincia(String provincia) {
        this.provincia = provincia;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isEmailInvitoInviata() {
        return emailInvitoInviata;
    }

    public void setEmailInvitoInviata(boolean emailInvitoInviata) {
        this.emailInvitoInviata = emailInvitoInviata;
    }

    public Agente getAgente() {
        return agente;
    }

    public void setAgente(Agente agente) {
        this.agente = agente;
    }

}
