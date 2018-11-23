package it.nextsw.common.persistence.entities;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;
import java.util.*;

@Entity
@DiscriminatorValue("AGENTE")
@Table(name = "agenti")
public class Agente extends PersistentObject {


    @Column(name = "NOME", nullable = true)
    private String nome;
    @Column(name = "COGNOME", nullable = true)
    private String cognome;

    @Column(name="SOSPESO", nullable = false)
    private boolean sospeso;


    @ManyToMany
    @JoinTable(name = "agente_settori", joinColumns=@JoinColumn(name="AGENTE_"+EntityConstants.DEFAULT_ID_COLUMN_NAME, referencedColumnName=EntityConstants.DEFAULT_ID_COLUMN_NAME),
            inverseJoinColumns=@JoinColumn(name="SETTORE_"+EntityConstants.DEFAULT_ID_COLUMN_NAME, referencedColumnName=EntityConstants.DEFAULT_ID_COLUMN_NAME))
    private List<SettoreOperativo> settori;

    @ManyToOne
    @JoinColumn(name="MERCATO_RIFERIMENTO_"+EntityConstants.DEFAULT_ID_COLUMN_NAME, nullable = true)
    private MercatoRiferimento mercatoRiferimento;

    @OneToMany(mappedBy = "agente", fetch = FetchType.LAZY)
    private List<MandatoAgente> mandati;


    @Column(name="TOTAL_AGENT_COINS", nullable = false)
    private int totalAgentCoins;

    // ============ Collections utili solo per la relazione del mapping, bloccate anche da intercaptor =================
    @OneToMany(mappedBy = "agente", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<ContattoRubrica> contatti;


//    @Embedded
//    private Indirizzo indirizzo;
    
    // ==== AREA OPERATIVA ======
    @OneToMany(mappedBy = "agente", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AreaOperativa> areeOperative;

//
    @Column(name = "LIVELLO_PRIVACY", nullable = false)
    @Enumerated(EnumType.STRING)
    private LivelloPrivacyAgente livelloPrivacyAgente;

    

    public Agente() {
    }

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

    public boolean isSospeso() {
        return sospeso;
    }

    public void setSospeso(boolean sospeso) {
        this.sospeso = sospeso;
    }

    public List<SettoreOperativo> getSettori() {
        return settori;
    }

    public void setSettori(List<SettoreOperativo> settori) {
        this.settori = settori;
    }


    public MercatoRiferimento getMercatoRiferimento() {
        return mercatoRiferimento;
    }

    public void setMercatoRiferimento(MercatoRiferimento mercatoRiferimento) {
        this.mercatoRiferimento = mercatoRiferimento;
    }

    public List<MandatoAgente> getMandati() {
        return mandati;
    }

    public void setMandati(List<MandatoAgente> mandati) {
        this.mandati = mandati;
    }

    public int getTotalAgentCoins() {
        return totalAgentCoins;
    }

    public void setTotalAgentCoins(int totalAgentCoins) {
        this.totalAgentCoins = totalAgentCoins;
    }

    public Set<ContattoRubrica> getContatti() {
        return contatti;
    }

    public void setContatti(Set<ContattoRubrica> contatti) {
        this.contatti = contatti;
    }

    public List<AreaOperativa> getAreeOperative() {
        return areeOperative;
    }

    public void setAreeOperative(List<AreaOperativa> areeOperative) {
        this.areeOperative = areeOperative;
    }

    public LivelloPrivacyAgente getLivelloPrivacyAgente() {
        return livelloPrivacyAgente;
    }

    public void setLivelloPrivacyAgente(LivelloPrivacyAgente livelloPrivacyAgente) {
        this.livelloPrivacyAgente = livelloPrivacyAgente;
    }
}
