package it.nextsw.common.persistence.entities;

import org.apache.log4j.Logger;

import javax.persistence.*;
import java.io.Serializable;


@MappedSuperclass
public abstract class PersistentObject implements Serializable {

    private  static final Logger logger = Logger.getLogger(PersistentObject.class);



    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


}
