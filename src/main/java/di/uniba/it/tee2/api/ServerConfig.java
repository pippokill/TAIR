/*
 * Questo software è stato sviluppato dal gruppo di ricerca SWAP del Dipartimento di Informatica dell'Università degli Studi di Bari.
 * Tutti i diritti sul software appartengono esclusivamente al gruppo di ricerca SWAP.
 * Il software non può essere modificato e utilizzato per scopi di ricerca e/o industriali senza alcun permesso da parte del gruppo di ricerca SWAP.
 * Il software potrà essere utilizzato a scopi di ricerca scientifica previa autorizzazione o accordo scritto con il gruppo di ricerca SWAP.
 * 
 * Bari, Marzo 2014
 */
package di.uniba.it.tee2.api;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 *
 * @author pierpaolo
 */
public class ServerConfig {

    private static ServerConfig instance;

    private Properties props;

    private ServerConfig() throws IOException {
        props = new Properties();
        props.load(new FileInputStream("./server.config"));
    }

    public synchronized static ServerConfig getInstance() throws IOException {
        if (instance == null) {
            instance = new ServerConfig();
        }
        return instance;
    }
    
    public String getProperty(String key) {
        return props.getProperty(key);
    }
    
    public int getInt(String key) {
        return Integer.parseInt(getProperty(key));
    }

}
