package edu.unidade2.cdn.impl;

import edu.unidade2.cdn.src.components.client.Cliente;
import edu.unidade2.cdn.src.config.IP;
import edu.unidade2.cdn.src.config.Ports;

public class ImplCliente {
    static void main() {
        new Cliente(
                IP.GEODNS_IP,
                Ports.ROUTER_UDP_PORT
        );
    }
}
