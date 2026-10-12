package edu.unidade2.cdn.impl;

import edu.unidade2.cdn.src.components.edge.EdgeServer;
import edu.unidade2.cdn.src.config.IP;
import edu.unidade2.cdn.src.config.Ports;

public class ImplEdgeServer {
    static void main() {
        new EdgeServer(
                IP.GEODNS_IP,
                Ports.ROUTER_TCP_PORT,
                IP.ORIGIN_IP,
                Ports.ORIGIN_SERVER_PORT,
                IP.EDGE_IP,
                Ports.CLIENT_PORT,
                Ports.EDGE_SERVER_PORT
        );
    }
}
