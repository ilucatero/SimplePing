package com.lucaterori;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/*** Created by Lucaterori on 10/02/2016.*/
public class MainLauncher {

    public static Logger LOGGER = Logger.getLogger(MainLauncher.class.getName());    //Set log file;

    private static Hashtable<String, Report> reports = new Hashtable<String, Report>();

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
        StringBuilder resp = new StringBuilder("\nDate:" + new Date()+"\n");

        if(!pType.equals(Config.PingType.TCP_UDP)) { // If is not a ping TCP/UDP

            try{
                resp.append( runInCommandLine( pingConfig.asParamList(pType,host) ) );
                //Check the type of command to set the message into the report
                if (pType.equals(Config.PingType.ICMP)){
                    rep.setIcmp_ping(resp.toString());
                }else if (pType.equals(Config.PingType.TR)) {
                    rep.setTrace(resp.toString());
                }
            } catch(Exception e){
                // If an error, send the report
                this.sendReport(pingConfig.getReportUrlServer(), rep);
            }

        }else { // If is a TCP_UDP then use HTTP Request

            try {
                Map responseData = this.sendHTTPData(host, "HEAD", null, pingConfig.getTimeout(), "text/html");

                int respCode = responseData != null ? (Integer) responseData.get("response_code") : 0;

                // Get the response from the connection
                resp.append( "\nURL: ").append( responseData != null ? responseData.get("url") : null);
                resp.append( "\nResponseCode: ").append( respCode );
                resp.append( "\nResponseMessage: " ).append( responseData != null ? responseData.get("response_message") : null );
                resp.append( "\nResponseTime (ms): " ).append( responseData != null ? responseData.get("response_time") : null );

               rep.setTcp_ping(resp.toString());

                if(respCode != HttpURLConnection.HTTP_OK){
                    this.sendReport(pingConfig.getReportUrlServer(), rep);
                }

            }catch (IOException e){
                LOGGER.warning(e.toString());
                resp.append(e);
            }
        }

        System.out.println(resp); // print the whole response in a single print to distinguish among threads
        return rep;
    }

    /**
     * Execute a command embed in a list along with its parameters.
     * @param command the command + parameter list
     * @return String The command execution result
     * @throws Exception
     */
    private String runInCommandLine(List<String> command) throws Exception {
        String s; // to store lines from stdInput
        StringBuilder resp = new StringBuilder();

        resp.append("\nExecuting:\n" ).append(command);

        // Execute the ping/tracert command via local terminal/commandline
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // read the command received response from the Inputstream
        resp.append("\nHere is the standard OUTPUT of the command:\n");
        while ((s = stdInput.readLine()) != null) {
            resp.append(s).append("\n");
        }

        // read any errors from the attempted command
        resp.append("\nHere is the standard ERROR of the command (if any):\n");
        while ((s = stdError.readLine()) != null) {
            resp.append(s).append("\n");
            throw new Exception(String.valueOf(resp));
        }

        return String.valueOf(resp);
    }

    /**
     * Send the report in Json format to a given url.
     * @param url the Url of the server to submit the json data
     * @param rep the Report where the json object can be extract
     * @return int The HTTP Response code
     * @throws Exception
     */
    public final int sendReport(String url, Report rep) throws Exception {

        int responseCode = 0;
        String json = rep.toJson();

        // Logging the report
        LOGGER.warning(json);

        try {
            Map responseData = this.sendHTTPData(url, "POST", json, 0, "application/json");

            responseCode = responseData != null ? (Integer) responseData.get("response_code") : 0;
            System.out.println("\nSending 'POST' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);
            System.out.println("ResponseMessage: " + (responseData != null ? responseData.get("response_message") : null));
            System.out.println("ResponseTime (ms): " + (responseData != null ? responseData.get("response_time") : null));

        }catch (IOException e){
            LOGGER.warning(e.toString());
        }

        return responseCode;
    }

    /**
     * Send data into a given URL.
     * @param url The url to send the request
     * @param httpMethod The HTTP method to use in the call. If HEAD http method is used, then it works as a tcp ping command.
     * @param data The data to be submited.
     * @param timeOut The time out for the response.
     * @return Map containing "url","response_code","response_message","response_time"
     * @throws IOException
     */
    private Map sendHTTPData(String url, String httpMethod, String data, int timeOut, String contentType) throws IOException {
        Map responseMap = new HashMap<>();
        String newUrl = new String(url);

        //TODO: this should be changed to actually validate and add the correct protocol (http|https)
        Pattern p = Pattern.compile("^(http|https)://?");
        System.out.println("Url (" + newUrl + ") starts with 'http(s)?' : "+ p.matcher(newUrl).find());
        if( !p.matcher(newUrl).find()){
            newUrl = "http://" + url;
        }

        HttpURLConnection connection = null;
        BufferedReader reader = null;
        try {

            // Send the HTTP request
            long startTime = System.currentTimeMillis();
            connection = (HttpURLConnection) new URL(newUrl).openConnection();

            // add connection conf
            if (timeOut > 0){
                connection.setConnectTimeout(timeOut);
                connection.setReadTimeout(timeOut);
            }
            connection.setDoOutput(true);
            connection.setDoInput(true);

            //add request header
            connection.setRequestMethod(httpMethod); // HEAD, GET, POST
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/json,application/xml;q=0.9,image/webp,*/*;q=0.8");

            connection.connect();

            if(data != null) {
                // Send post request
                OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
                wr.write(data);
                wr.flush();
                wr.close();
            }
            // read the output from the server
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line = null;

            while ((line = reader.readLine()) != null){
                stringBuilder.append(line).append("\n");
            }

            long elapsedTime = System.currentTimeMillis() - startTime;

            responseMap.put("url", connection.getURL().toString());
            responseMap.put("response_code", connection.getResponseCode());
            responseMap.put("response_message", connection.getResponseMessage());
            responseMap.put("response_time", elapsedTime+"");

        } catch (MalformedURLException | ProtocolException e){
            LOGGER.warning("\nInvalid URL: " + e);
            throw e;
        } catch (IOException e) {
            LOGGER.warning("\n Error while processing the http call : " + e);
            throw e;
        } finally {
            // close the reader; this can throw an exception too, so
            // wrap it in another try/catch block.
            if (reader != null){
                try{
                    reader.close();
                } catch (IOException ioe){
                    ioe.printStackTrace();
                }
            }
            if (connection != null){
                connection.disconnect();
            }
        }


        return responseMap;
    }


}
