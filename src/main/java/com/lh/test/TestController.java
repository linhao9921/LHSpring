package com.lh.test;

import com.lh.annotation.LHAutowrited;
import com.lh.annotation.LHController;
import com.lh.annotation.LHRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by Linhao on 2018/4/22.
 */
@LHController
@LHRequestMapping("/test")
public class TestController {

    @LHAutowrited
    private ITestService testService;

    @LHRequestMapping("/query")
    public void query(HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter out = response.getWriter();
        out.write(testService.query());

        out.flush();
        out.close();
    }
}
