package uk.antiperson.stackmob.tools.cache;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import uk.antiperson.stackmob.StackMob;
import uk.antiperson.stackmob.tools.extras.GlobalValues;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Template for future SQL caching.

public class SQLCache implements Cache{

    private String username;
    private String password;
    private String finalUrl;
    private Connection con;
    private StackMob sm;
    public SQLCache(StackMob sm){
        String serverUrl = sm.config.getCustomConfig().getString("caching.mysql.server-ip");
        int serverPort = sm.config.getCustomConfig().getInt("caching.mysql.server-port");
        username = sm.config.getCustomConfig().getString("caching.mysql.username");
        password = sm.config.getCustomConfig().getString("caching.mysql.password");
        finalUrl = "jdbc:mysql://" + serverUrl + ":" + serverPort + "/stackmob?autoReconnect=true&useSSL=false";
        this.sm = sm;
    }


    public int read(UUID uuid){
        try{
            ResultSet rs = con.createStatement().executeQuery("SELECT Size FROM CACHE WHERE UUID='" + uuid.toString() + "';");
            rs.next();
            return rs.getInt(1);
        }catch (SQLException e){
            e.printStackTrace();
        }
        return 0;
    }

    public void write(UUID uuid, int value){
        remove(uuid);
        try {
            con.createStatement().execute("INSERT INTO CACHE VALUES ('" + uuid.toString() + "', " + value + ");");
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public boolean contains(UUID uuid){
        try {
            return con.createStatement().executeQuery("SELECT UUID FROM CACHE WHERE UUID='" + uuid.toString()+ "';").next();
        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    public void remove(UUID uuid){
        try {
            con.createStatement().execute("DELETE FROM CACHE WHERE UUID='" + uuid.toString() + "';");
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void load(){
        try{
            inialize();
            con.createStatement().execute("CREATE TABLE IF NOT EXISTS CACHE (UUID varchar(255), Size int);");
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            for(World world : Bukkit.getWorlds()){
                for(Entity entity : world.getEntities()){
                    if(entity.hasMetadata(GlobalValues.METATAG) &&
                            entity.getMetadata(GlobalValues.METATAG).size() > 0 &&
                            entity.getMetadata(GlobalValues.METATAG).get(0).asInt() > 1){
                        write(entity.getUniqueId(), entity.getMetadata(GlobalValues.METATAG).get(0).asInt());
                    }else if(entity.hasMetadata(GlobalValues.NOT_ENOUGH_NEAR) &&
                            entity.getMetadata(GlobalValues.NOT_ENOUGH_NEAR).size() > 0 &&
                            entity.getMetadata(GlobalValues.NOT_ENOUGH_NEAR).get(0).asBoolean()){
                        write(entity.getUniqueId(), -69);
                    }
                }
            }

            con.close();
        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public Set<UUID> getKeys(){
        HashSet<UUID> keys = new HashSet<>();
        try{
            ResultSet rs = con.createStatement().executeQuery("SELECT UUID FROM CACHE;");
            while (rs.next()){
                keys.add(UUID.fromString(rs.getString(1)));
            }
        }catch (SQLException e){
            e.printStackTrace();
        }

        return keys;
    }

    public void inialize() throws SQLException{
        con = DriverManager.getConnection(finalUrl, username, password);
    }
}