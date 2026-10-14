package com.scc.model;

public record User( int user_id, String full_name, String username, String password , String role, String status,  String date_created){
    @Override
    public  String toString(){
        return String.format("[%d] %s (%s) - Status: %s, " , user_id ,username ,role , status );

    }
}
