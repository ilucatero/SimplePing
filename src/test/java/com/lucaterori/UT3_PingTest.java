package com.lucaterori;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

/**
 * Created by Lucaterori on 11/02/2016.
 */
public final class UT3_PingTest {
    String[] args;
    Config config;
    List<String> hosts;
    MainLauncher ml = new MainLauncher("src/test/TestLog.log");

    public UT3_PingTest(){
        // Used TEST Server : http://httpbin.org/
        args = "-n 5 -hf .\\src\\test\\hostList.txt -log log.log -d 10000 -w 5000 -r http://httpbin.org/post ".split(" ");
        config = new Config(args);
    }

    public void setUp(){}
    public void tearDown(){}

    public void test1DoCommandMethod() {
        System.out.println("1:test1DoCommandMethod");
        try{
            String host = "jasmin.com";
            Report rep = new Report(host);
            Report repRes;

            repRes = ml.doCommand(config, Config.PingType.ICMP, host, rep);
            assert repRes.getIcmp_ping() != null && !repRes.getIcmp_ping().isEmpty();

            repRes = ml.doCommand(config, Config.PingType.TCP_UDP, host, rep);
            assert repRes.getTcp_ping() != null && !repRes.getTcp_ping().isEmpty();

            repRes = ml.doCommand(config, Config.PingType.TR, host, rep);
            assert repRes.getTrace() != null && !repRes.getTrace().isEmpty();

            assert true;
        }catch(Exception e){
            assert false; // If error the test is unsuccessful
        }
    }

    public void test2SendReportMethod() {
        System.out.println("2:test2SendReportMethod");
        try{
            String host = "jasmin.com";
            Report rep = new Report(host);

            System.out.println("------------------SERVER:"+ ml.sendReport(config.getReportUrlServer(), rep));

            assert ml.sendReport(config.getReportUrlServer(), rep) == HttpURLConnection.HTTP_OK;

        }catch(Exception e){
            assert false; // If error the test is unsuccessful
        }
    }

/*    public void test3ComputeHostMethod()  {
        System.out.println("3:test3ComputeHostMethod");
        try{
            ml.computeHosts(config, Files.readAllLines(Paths.get(config.getHosts_File())));

            assert true;
        }catch(Exception e){
            assert false; // If error the test is unsuccessful
        }
    }*/

/*    public void test4MainMethod() {
        System.out.println("4:test4MainMethod");
        try{
            ml.main(args);
            assert true;
        }catch(Exception e){
            assert false; // If error the test is unsuccessful
        }
    }*/


}
