package com.lucaterori;

import java.util.ArrayList;
import java.util.List;

/** Created by Lucaterori on 10/02/2016. */
public final class Config {
    enum PingType {
        TCP_UDP, ICMP, TR;
    }
    private String hosts_File;
    private String log_File = "./Logging.log";
    private Integer delay = 5000;
    private Integer count = 4;
    private Integer timeout = 5000;
    private String reportUrlServer;

    public Config(String[] args) {
        for (int i = 0; i < args.length-1; i++) {
            switch (args[i].toLowerCase()){
                case "-hf" : this.hosts_File =  args[++i]; break;
                case "-n" : this.count =  new Integer(args[++i]); break;
                case "-d" : this.delay =  new Integer(args[++i]); break;
                case "-w" : this.timeout =  new Integer(args[++i]); break;
                case "-r" : this.reportUrlServer =  args[++i]; break;
                case "-log" : this.log_File =  args[++i]; break;

                /*case "-p" : {
                    ++i;
                    switch (args[i].toLowerCase()){
                        case "tcp":  this.pingType = PingType.TCP_UDP; break;
                        case "tr": this.pingType = PingType.TR; break;
                        case "icmp": this.pingType = PingType.ICMP; break;
                        default: break;
                    }
                    break;
                }*/
                default:
                    break;
            }
        }


        if(this.getHosts_File() == null){
            System.out.println("Error: The HOST FILE is not set.");
            Config.printMan();
            System.exit(1);
        }
    }

    public String getHosts_File() {
        return hosts_File;
    }
    public String getLog_File() {
        return log_File;
    }

    public Integer getDelay() {
        return delay;
    }

    public Integer getCount() {
        return count;
    }


    public Integer getTimeout() {
        return timeout;
    }
    public String getReportUrlServer() {
        return reportUrlServer;
    }


    @Override
    public String toString() {
        return "PingConf:{HostFile:"+getHosts_File()
                + ",logFile:"+getLog_File()
                + ",delay:"+getDelay()
                + ",count:"+getCount()
                + ",timeout:"+getTimeout()
                + ",reportUrlServer:"+getReportUrlServer()
                + "}";
    }

    /**
     * Compute the command to Post/Tracer (depending on current PingType) for a given host
     * @param host The given host to generate the Ping/Tracer command
     * @return List containing the list of parameters including the specific command
     */
    public final List<String> asParamList(PingType pType, String host){
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

        if(pType.equals(PingType.ICMP) ) {
            List<String> params = new ArrayList();
            params.add("ping");
            params.add(isWindows ? "-n" : "-c");
            params.add(this.getCount()+"");
            params.add("-w");
            params.add(this.getTimeout()+"");
            params.add(host);
            return params;
        } else if( pType.equals(PingType.TR)) {
            List<String> params = new ArrayList();
            params.add("tracert");
            params.add("-w");
            params.add(this.getTimeout()+"");
            params.add(host);
            return params;
        }

        return null;
    }

    /**
     * Used to print the parameters recognized by the application
     */
    public static void printMan(){
        System.out.println("Simple Ping\n"
                + "     -p      Protocol            [icmp(default),tcp,tr(tracert)] \n"
                + "     -n      Count               Number of requests \n"
                + "     -hf     Hosts File          List of host to request \n"
                + "     -d      Delay               Time to wait between scheduled request list \n"
                + "     -w      TimeOut             (TCP only) time to wait for response \n"
                + "     -r      Report Url          URL to send the report \n"
                + "     -log    Log File            Log file to be use (logging.log as default) \n"
                + " \nExample: -p icmp -hr C:\\Hosts.txt -n 5 -d 3 -log logs.log"
        );
    }
}
