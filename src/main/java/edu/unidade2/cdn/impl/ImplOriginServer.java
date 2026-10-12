package edu.unidade2.cdn.impl;

import edu.unidade2.cdn.src.components.origin.OriginServer;
import edu.unidade2.cdn.src.config.IP;
import edu.unidade2.cdn.src.config.Ports;

public class ImplOriginServer {
    static void main() {
        new OriginServer(
                Ports.ORIGIN_SERVER_PORT,
                IP.EDGE_IP,
                Ports.EDGE_SERVER_PORT
        );
    }
}
