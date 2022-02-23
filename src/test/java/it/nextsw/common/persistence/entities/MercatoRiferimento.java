package it.nextsw.common.persistence.entities;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "mercati_riferimento")
public class MercatoRiferimento extends PersistentObject {


    @Column(name ="NOME", nullable = false, length= EntityConstants.VARCHAR_FIELD_MEDIUM)
    private String nome;


    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
}
