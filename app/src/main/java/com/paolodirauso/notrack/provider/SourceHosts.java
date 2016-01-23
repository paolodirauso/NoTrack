package com.paolodirauso.notrack.provider;

import java.util.ArrayList;

public class SourceHosts {
    private ArrayList<String> sources;

    public SourceHosts(){
        sources = new ArrayList<>();
        sources.add("http://www.paolodirauso.com/hosts.txt");
    }

    public ArrayList<String> getSources(){
        return sources;
    }
}
