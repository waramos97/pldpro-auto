package br.cepel.dpc.pldpro_auto.domain;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "host")
public class Host {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hostName", nullable = false)
    private String hostName;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "userName", nullable = false)
    private String userName;

    @Column(name = "bootstrap",nullable = false)
    private Boolean bootstrap;

    @OneToMany(mappedBy = "host", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Caso> casos;

    public Host(Long id, String hostName, Integer port, String userName, Boolean bootstrap, List<Caso> casos) {
        this.id = id;
        this.hostName = hostName;
        this.port = port;
        this.userName = userName;
        this.bootstrap = bootstrap;
        this.casos = casos;
    }

    public Host() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Boolean getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(Boolean bootstrap) {
        this.bootstrap = bootstrap;
    }

    public List<Caso> getCasos() {
        return casos;
    }

    public void setCasos(List<Caso> casos) {
        this.casos = casos;
    }
}
