package com.lucaterori;

/** Created by Lucaterori on 10/02/2016. */
public final class Report {

    private String host;
    private String icmp_ping;
    private String tcp_ping;
    private String trace;

    public Report(String host) {
        this.host = host;
    }

    public String getTrace() {
        return trace;
    }
    public void setTrace(String trace) {
        this.trace = trace;
    }

    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }

    public String getIcmp_ping() {
        return icmp_ping;
    }
    public void setIcmp_ping(String icmpPing) {
        this.icmp_ping = icmpPing;
    }

    public String getTcp_ping() {
        return tcp_ping;
    }
    public void setTcp_ping(String tcpPing) {
        this.tcp_ping = tcpPing;
    }

    /**
     * Convert the Report data into JSON format
     * @return String containing the converted Report data into Json format
     */
    public String toJson() {
        return   "{\"host\":\"" + host + "\"," +
                  "\"icmp_ping\":\"" + icmp_ping + "\","
                + "\"tcp_ping\":\"" + tcp_ping + "\","
                + "\"trace\":\"" + trace + "\"}";

    }

}