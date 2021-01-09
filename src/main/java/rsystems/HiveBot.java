package rsystems;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.JDAImpl;
import rsystems.events.*;
import rsystems.handlers.*;
import rsystems.objects.DBPool;
import rsystems.objects.Dispatcher;

import javax.security.auth.login.LoginException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class HiveBot{
    public static String prefix = Config.get("prefix");
    public static String karmaPrefixPositive = Config.get("KARMA_PREFIX_POS");
    public static String karmaPrefixNegative = Config.get("KARMA_PREFIX_NEG");
    public static String version = "0.19.13";

    public static DBPool dbPool = new DBPool(Config.get("DATABASE_HOST"), Config.get("DATABASE_USER"),Config.get("DATABASE_PASS"));
    public static SQLHandler sqlHandler = new SQLHandler(dbPool.getPool());
    public static KarmaSQLHandler karmaSQLHandler = new KarmaSQLHandler(dbPool.getPool());

    public static DataFile dataFile = new DataFile();
    public static Dispatcher dispatcher;

    public static References references;

    public static JDAImpl jda = null;

    public static Guild drZzzGuild(){
        return jda.getGuildById(Config.get("GUILD_ID"));
    }

    //AuthMap used for command authorization
    public static Map<Long,Integer> authMap = new HashMap<>();

    public static Map<Long, ArrayList<String>> emojiPerkMap = new HashMap<>();

    public static boolean debug = Boolean.parseBoolean(Config.get("DEBUG"));


    //Initiate Loggers
    public final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static void main(String[] args) throws LoginException, SQLException {
        JDA api = JDABuilder.createDefault(Config.get("token"))
                .enableIntents(GatewayIntent.GUILD_MEMBERS,GatewayIntent.GUILD_PRESENCES)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.ACTIVITY)
                .setChunkingFilter(ChunkingFilter.ALL)
                .build();

        api.addEventListener(dispatcher = new Dispatcher());
        api.addEventListener(new GuildMemberJoin());
        api.addEventListener(new GratitudeListener());
        //api.addEventListener(new TestEvent());
        api.addEventListener(new NicknameListener());
        api.addEventListener(references = new References());

        References.loadReferences();

        api.getPresence().setStatus(OnlineStatus.ONLINE);
        api.getPresence().setActivity(Activity.playing(Config.get("activity")));

        try {
            // Wait for discord jda to completely load
            api.awaitReady();
            jda = (JDAImpl) api;

            HiveBot.authMap.putIfAbsent(Long.valueOf("620805075190677514"),65535);
            HiveBot.sqlHandler.loadPerkEmojis();

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

