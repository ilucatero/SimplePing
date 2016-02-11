package com.lucaterori;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by Wizard on 11/02/2016.
 */
public final class UT2_ReprotTest {
    Report rep;

    public UT2_ReprotTest(){
        rep = new Report("jasmin.com");
    }

    public void setUp(){}
    public void tearDown(){}

    public void test1DataSetUp(){
        System.out.println("1:test1DataSetUp");

        assert rep.getHost() != null; //check if the constructor has insert the host

        rep.setTrace("result lines of the last trace command");
        rep.setTcp_ping("result lines of the last tcp ping command");
        rep.setIcmp_ping("result lines of the last icmp ping command");
        rep.setHost("oranum.com");

        assert rep.getHost() != null && !rep.getHost().isEmpty();
        assert rep.getIcmp_ping() != null && !rep.getIcmp_ping().isEmpty();
        assert rep.getTcp_ping() != null && !rep.getTcp_ping().isEmpty();
        assert rep.getTrace() != null && !rep.getTrace().isEmpty();

    }

    public void test2getJson(){
        System.out.println("2:test2getJson" );

        assert rep.toJson() != null && !rep.toJson().isEmpty();
        System.out.println(rep.toJson());
    }




}
