package edu.unidade2.servicediscovery.impl;

import edu.unidade2.servicediscovery.src.client.Client;
import edu.unidade2.servicediscovery.src.server.proxy.DirectoryServer;

public class ImplClient {
    public static void main(String [] args) {
        new Client("localhost", DirectoryServer.BASE_PORT);
    }
}
