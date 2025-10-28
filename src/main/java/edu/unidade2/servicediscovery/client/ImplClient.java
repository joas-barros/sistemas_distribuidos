package edu.unidade2.servicediscovery.client;

import edu.unidade2.servicediscovery.server.proxy.DirectoryServer;

public class ImplClient {
    static void main() {
        new Client("localhost", DirectoryServer.BASE_PORT);
    }
}
