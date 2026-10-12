package com.scc.model;

public record User( int id,String username, String password , String full_name, String role, String status,  String date_created){
    @Override
    public  String toString(){
        return String.format("[%d] %s (%s) - Status: %s, " , id ,username ,role , status );

    }
}
