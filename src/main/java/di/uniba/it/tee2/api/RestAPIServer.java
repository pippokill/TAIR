/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.api;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/**
 *
 * @author pierpaolo
 */
public class RestAPIServer extends Thread {

    private final HttpServer server;

    protected HttpServer startServer() throws IOException {
        // create a resource config that scans for JAX-RS resources and providers
        // in com.example package
        final ResourceConfig rc = new ResourceConfig()
                .packages(true, "di.uniba.it.tee2.api.v1");

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(ServerConfig.getInstance().getProperty("bind.address")), rc);
    }

    public RestAPIServer() throws Exception {
        //init wrapper
        System.out.println("Init wrapper");
        SearchServiceWrapper.getInstance(ServerConfig.getInstance().getProperty("search.language"),
                ServerConfig.getInstance().getProperty("search.index"));
        server = startServer();
        System.out.println(String.format("Jersey app started with WADL available at "
                + "%sapplication.wadl", ServerConfig.getInstance().getProperty("bind.address")));
        //attach a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            //this method is run when the service is stopped (SIGTERM)
            @Override
            public void run() {
                try {
                    SearchServiceWrapper.getInstance(ServerConfig.getInstance().getProperty("search.language"),
                            ServerConfig.getInstance().getProperty("search.index")).getSearch().close();
                    server.shutdownNow();
                } catch (Exception ex) {
                    Logger.getLogger(RestAPIServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }));
    }

    @Override
    public void run() {
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(RestAPIServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            RestAPIServer service = new RestAPIServer();
            service.start();
        } catch (Exception ex) {
            Logger.getLogger(RestAPIServer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
