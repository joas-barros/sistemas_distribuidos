package edu.unidade2.cdn.impl;

import edu.unidade2.cdn.src.components.router.GeoDNS;
import edu.unidade2.cdn.src.config.Ports;

public class ImplGeoDNS {
    static void main() {
        new GeoDNS(
                Ports.ROUTER_UDP_PORT,
                Ports.ROUTER_TCP_PORT
        );
    }
}
