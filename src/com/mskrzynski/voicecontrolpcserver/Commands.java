package com.mskrzynski.voicecontrolpcserver;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
//nazwa klasy identyczna z tabelą w bazie danych
public class Commands {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY) //automatycznie generuj id na podstawie IDENTITY
    @SuppressWarnings("unused")
    private int id; //nazwa kolumny
    private String wyraz; //nazwa kolumny
    private String polecenie; //nazwa kolumny

    String getWyraz() {
        return wyraz;
    }

    void setWyraz(String wyraz) {
        this.wyraz = wyraz;
    }

    String getPolecenie() {
        return polecenie;
    }

    void setPolecenie(String polecenie) {
        this.polecenie = polecenie;
    }
}
