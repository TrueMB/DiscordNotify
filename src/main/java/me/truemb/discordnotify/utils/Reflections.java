package me.truemb.discordnotify.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Reflections {

    public static Class< ? > getNMSClass( String name ) {
        String version = Bukkit.getServer().getClass().getPackage().getName().split( "\\." )[ 3 ];
        try {
            return Class.forName( "net.minecraft.server." + version + "." + name );
        } catch ( Exception e ) {
            return null;
        }
    }

    public static Class< ? > getBukkitClass( String name ) {
        try {
            String version = Bukkit.getServer().getClass().getPackage().getName().split( "\\." )[ 3 ];
            return Class.forName( "org.bukkit.craftbukkit." + version + "." + name );
        } catch( Exception ex ) {
            return null;
        }
    }

    public static void sendPacket( Player to, Object packet ) {
        try {
            Object playerHandle = to.getClass().getMethod( "getHandle" ).invoke( to );
            Object playerConnection = playerHandle.getClass().getField( "playerConnection" ).get( playerHandle );
            playerConnection.getClass().getMethod( "sendPacket", getNMSClass( "Packet" ) ).invoke( playerConnection, packet );
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    public static void setField( Object change, String name, Object to ) {
        try {
            Field field = getField( change.getClass(), name );
            field.setAccessible(true);
            field.set( change, to );
            field.setAccessible(false);
        } catch( Exception ex ) {
            ex.printStackTrace();
        }
    }

    public static void setAccessible( boolean state, Field... fields ) {
        try {
            for( Field field : fields ) {
                field.setAccessible( state );
            }
        } catch( Exception ex ) {
            ex.printStackTrace();
        }
    }
    
    public static Field getField( Class< ? > clazz, String name ) {
        Field field = null;

        for( Field f : getFields( clazz ) ) {
            if( !f.getName().equals( name ) )
                continue;

            field = f;
            break;
        }

        return field;
    }

    public static List< Field > getFields( Class< ? > clazz ) {
        List< Field > buf = new ArrayList<>();
        do {
            try {
                for( Field f : clazz.getDeclaredFields() ) {
                    buf.add( f );
                }
            } catch( Exception ex ) {}
        } while( ( clazz = clazz.getSuperclass() ) != null );

        return buf;
    }

    public String getVersion() {
        String ver = Bukkit.getServer().getClass().getPackage().getName().split( "\\." )[ 3 ];
        return ver;
    }
}
