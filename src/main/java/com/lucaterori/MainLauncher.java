package com.lucaterori;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by Wizard on 10/02/2016.
 */
public class MainLauncher {

    public static Logger LOGGER = Logger.getLogger(MainLauncher.class.getName());    //Set log file;

    private static Hashtable<String, Report> reports = new Hashtable();

    private MainLauncher(){}

    public MainLauncher(String logFile){
        //Set logger file
        try {
            Handler fh = new FileHandler(logFile);
            LOGGER.addHandler(fh);
        } catch (SecurityException | IOException  e) {
            LOGGER.severe("Impossible to set the FileHandler ");
        }
    }

    public static void main(String args[]) throws Exception {

        if(args.length == 0) {Config.printMan(); System.exit(1);}

        Config pingConfig = new Config(args); // Load conf from arguments
        System.out.println("Given Configuration:\n" + pingConfig.toString());

        List<String> hosts = Files.readAllLines(Paths.get(pingConfig.getHosts_File()));
        System.out.println("Given hosts:\n" + hosts.toString());

        System.out.println("============================Start computing hosts========================\n");

        // create the object and adds the logger filename at startup
        MainLauncher ping = new MainLauncher(pingConfig.getLog_File());
        // launch the ping/tracert command using the hosts list
        ping.computeHosts(pingConfig, hosts);

        // print the last reports in case th user uses the ^C to terminate the programe.
        // Note: if is terminated by SIGKILL it won't work
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("------SHUTDOWN Hosts Computing-------------------------------------------");
            System.out.println("Printing last mem stored reports");
            reports.forEach((k, v) -> {
                System.out.println(v.toJson());
            });
        }));
    }

    /**
     * @param pingConfig The command and arguments you want to execute, provided as List<String>.
     * @param hosts The list of hosts that the PING / TRACE ROUTE will use.
     * @throws Exception This exception is thrown so you will know about it, and can deal with it.
     */
    public final void computeHosts(Config pingConfig, List<String> hosts) throws Exception {
        // to launch each host on a thread using a scheduler: ScheduledExecutorService
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(hosts.size());

        hosts.stream().forEach(host -> {
            Report rep = new Report(host);
            reports.put(host, rep);
            Runnable task = () -> {
                try {
                    this.doCommand(pingConfig, Config.PingType.ICMP, host, rep); // Run ICMP Ping
                    this.doCommand(pingConfig, Config.PingType.TCP_UDP, host, rep); // Run TCP Ping
                    this.doCommand(pingConfig, Config.PingType.TR, host, rep); // Run Tracert

                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            };

            executor.scheduleAtFixedRate(task, 0, pingConfig.getDelay(), TimeUnit.MILLISECONDS);
        });

        //TimeUnit.MILLISECONDS.sleep(1337);

    }


    /**
     * @param pingConfig The command and arguments you want to execute, provided as List<String>.
     * @param host The hosts that the PING / TRACE ROUTE will use.
     * @param rep  The report data (icmpPing, tcpPing, traceroute) container.
     * @throws Exception This exception is thrown so you will know about it, and can deal with it.
     */
    public final Report doCommand(Config pingConfig, Config.PingType pType, String host, Report rep) throws Exception {
        String s; // to store lines from stdInput
        StringBuilder resp = new StringBuilder("\nDate:" + new Date()+"\n");

        if(!pType.equals(Config.PingType.TCP_UDP)) { // If is not a ping TCP/UDP
            resp.append("\nExecuting:\n" + pingConfig.asParamList(pType, host));

            // Execute the ping/tracert command via local terminal/commandline
            ProcessBuilder pb = new ProcessBuilder(pingConfig.asParamList(pType,host));
            Process process = pb.start();

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            // read the command received response from the Inputstream
            resp.append("\nHere is the standard OUTPUT of the command:\n");
            while ((s = stdInput.readLine()) != null) {
                resp.append(s+"\n");
            }

            if (pType.equals(Config.PingType.ICMP)){
                rep.setIcmp_ping(resp.toString());
            }else if (pType.equals(Config.PingType.TR)) {
                rep.setTrace(resp.toString());
            }

            // read any errors from the attempted command
            resp.append("\nHere is the standard ERROR of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                resp.append(s+"\n");
                this.sendReport(pingConfig.getReportUrlServer(), rep);
            }
        }else { // If is a TCP_UDP then use HTTP Request
            try {
                //TODO: this should be changed to actually validate and add the correct protocol (http|https)
                Pattern p = Pattern.compile("(http:\\/\\/|https:\\/\\/)?");
                if( !p.matcher(host).matches()){
                    host = "http://" + host;
                }

                // Send the HTTP request
                long startTime = System.currentTimeMillis();
                HttpURLConnection con = (HttpURLConnection) new URL(host).openConnection();
                con.setRequestMethod("HEAD");
                con.setConnectTimeout(pingConfig.getTimeout());
                con.setReadTimeout(pingConfig.getTimeout());
                con.connect();
                long elapsedTime = System.currentTimeMillis() - startTime;

                // Get the response from the connection
                resp.append( "\nURL: "+ host
                    + "\nResponseCode: "+ con.getResponseCode()
                    + "\nResponseTime (ms): "+ elapsedTime );

               rep.setTcp_ping(resp.toString());

                if(con.getResponseCode() != HttpURLConnection.HTTP_OK){
                    this.sendReport(pingConfig.getReportUrlServer(), rep);
                }

                con.disconnect();

            }catch (MalformedURLException e){
                resp.append("\nInvalid URL: " + e);
            }
        }

        System.out.println(resp); // print the whole response in a single print to distinguish among threads
        return rep;
    }

    /**
     * Send the report in Json format to a given url.
     * @param url the Url of the server to submit the json data
     * @param rep the Report where the json object can be extract
     * @return int The HTTO Response code
     * @throws Exception
     */
    public final int sendReport(String url, Report rep) throws Exception {
        int responseCode;
        BufferedReader in;
        StringBuilder response = new StringBuilder();
        String inputLine;

        String json = rep.toJson();

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // Logging the report
        LOGGER.warning(json);

        //add request header
        con.setRequestMethod("POST");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");

        // Send post request
        con.setDoOutput(true);
        con.setDoInput(true);
        OutputStreamWriter wr = new OutputStreamWriter(con.getOutputStream());
        wr.write(json);
        wr.flush();
        wr.close();

        // Get the response from the connection
        responseCode = con.getResponseCode();
        System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);

        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        con.disconnect();

        //print result
        System.out.println("\nResponse: " + response);
        return responseCode;
    }


}
