package com.lh.test;

import com.lh.annotation.LHService;

/**
 * Created by Linhao on 2018/4/22.
 */
@LHService("testService")
public class TestService implements ITestService{
    @Override
    public String query() {
        return "LH Spring";
    }
}
