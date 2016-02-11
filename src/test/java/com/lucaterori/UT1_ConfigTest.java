package com.lucaterori;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by Wizard on 11/02/2016.
 */
public final class UT1_ConfigTest {
    String[] args;
    Config config;
    List<String> hosts;

    public UT1_ConfigTest(){
        args = "-n 5 -hf .\\src\\test\\hostList.txt -log log.log -d 10000 -w 5000 -r http:\\\\localhost:8080\\myPostService".split(" ");
        config = new Config(args);
    }

    public void setUp(){
        System.out.println("S--------------------");
    }
    public void tearDown(){}

    public void test1DataSetUp(){

        System.out.println("1:test1DataSetUp");

        assert config.getLog_File() != null && !config.getLog_File().isEmpty();
        assert config.getReportUrlServer() != null && !config.getReportUrlServer().isEmpty();
        assert config.getHosts_File() != null && !config.getHosts_File().isEmpty();
        assert config.getCount() != null ;
        assert config.getDelay() != null;
        assert config.getTimeout() != null;
    }

    public void test2HostFile() throws IOException {
        System.out.println("2:test2HostFile");

        hosts = Files.readAllLines(Paths.get(config.getHosts_File()));
        assert hosts != null;
        assert hosts.size() > 0;
    }

    public void test3ParamsListMethod() throws IOException {
        System.out.println("3:test3ParamsListMethod");

        hosts = Files.readAllLines(Paths.get(config.getHosts_File()));
        hosts.stream().forEach(host -> {
            assert config.asParamList(Config.PingType.ICMP, host).size() > 0;
            assert config.asParamList(Config.PingType.TCP_UDP, host).size() > 0;
            assert config.asParamList(Config.PingType.TR, host).size() > 0;}

        );
    }

    public void test4CorrectlyExecMethods() throws IOException {
        System.out.println("4:test4CorrectlyExecMethods");
        try{
            config.printMan();
            config.toString();

            assert true;
        }catch(Exception e){
            assert false; // If error the test is unsuccessful
        }

    }


}
