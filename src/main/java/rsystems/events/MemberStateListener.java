package rsystems.events;

import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import rsystems.Config;
import rsystems.HiveBot;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class MemberStateListener extends ListenerAdapter {

    public void onGuildMemberJoin(GuildMemberJoinEvent event){

        checkNickname(event.getGuild(), event.getMember());

        try {
            if(HiveBot.karmaSQLHandler.getKarma(event.getMember().getId()) == null){
                System.out.println("Adding member: " + event.getMember().getId());
                if(HiveBot.karmaSQLHandler.insertUser(event.getMember().getId(),event.getMember().getUser().getAsTag())){
                    System.out.println("success!");
                } else {
                    System.out.println("failed to add member");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        String greetingMessage = null;
        try {
            greetingMessage = HiveBot.database.grabRandomGreeting();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if(greetingMessage != null){

            greetingMessage = greetingMessage.replace("{user}",String.format("**%s**",event.getMember().getEffectiveName()));

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Welcome")
                    .setThumbnail(event.getMember().getUser().getEffectiveAvatarUrl())
                    .setColor(HiveBot.getColor(HiveBot.colorType.USER))
                    .setFooter("ID: " + event.getUser().getId())
                    .setDescription(greetingMessage);

            if(event.getUser().getAvatarUrl() == null){
                embedBuilder.appendDescription("\n\nWe encourage all members to use a custom avatar here on discord.  However this is __**NOT**__ required.");
            }

            TextChannel welcomeChannel = HiveBot.mainGuild().getTextChannelById(Config.get("WELCOME_CHANNEL"));
            if(welcomeChannel != null){
                welcomeChannel.sendMessageEmbeds(embedBuilder.build()).queue();
            }
            embedBuilder.clear();
        }
    }

    @Override
    public void onGuildMemberUpdateNickname(@NotNull GuildMemberUpdateNicknameEvent event) {
        checkNickname(event.getGuild(), event.getMember());
    }

    private void checkNickname(final Guild guild, final Member member){
        final String nickname = member.getEffectiveName();
        if(!EmojiParser.extractEmojis(nickname).isEmpty()){
            String newNickname = EmojiParser.removeAllEmojis(nickname);
            try{
                if(!newNickname.equalsIgnoreCase(nickname)) {
                    guild.modifyNickname(member, newNickname).reason("Removing emoji's per server TOS").queue();
                }
            } catch (Exception e){
                System.out.println("An error occurred when trying to edit nickname for: " + member.getUser().getAsTag());
            }
        }
    }

    @Override
    public void onUserUpdateOnlineStatus(UserUpdateOnlineStatusEvent event) {
        if (event.getUser().isBot()) {
            return;
        }

        checkNickname(event.getGuild(), event.getMember());

        if (event.getNewOnlineStatus().equals(OnlineStatus.ONLINE)) {
            /*
            KARMA SYSTEM
             */

            //Initiate the formatter for formatting the date into a set format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy");
            //Get the current date
            LocalDate currentDate = LocalDate.now();
            //Format the current date into a set format
            String formattedCurrentDate = formatter.format(currentDate);

            //Get the last date of karma increment
            String lastSeenKarma = null;
            try {
                lastSeenKarma = HiveBot.karmaSQLHandler.getDate(event.getMember().getId());
            } catch (SQLException e) {
                e.printStackTrace();
            }

            //Insert new user if not found in DB
            if (lastSeenKarma.isEmpty()) {
                /*if (karmaSQLHandler.insertUser(event.getMember().getId(), event.getUser().getAsTag(), formattedCurrentDate, "KARMA")) {
                    System.out.println("Failed to add member to database");
                } else {
                    karmaSQLHandler.overrideKarmaPoints(event.getMember().getId(), 5);
                }
                 */
            } else {
                long daysPassed = ChronoUnit.DAYS.between(LocalDate.parse(lastSeenKarma, formatter), currentDate);
                if (daysPassed >= 1) {
                    try {
                        HiveBot.karmaSQLHandler.addKarmaPoints(event.getMember().getId(), formattedCurrentDate, false);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}