package rsystems.slashCommands.user;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import rsystems.HiveBot;
import rsystems.objects.KarmaUserInfo;
import rsystems.objects.SlashCommand;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class GetKarma extends SlashCommand {
    @Override
    public void dispatch(User sender, MessageChannel channel, String content, SlashCommandEvent event) {
        event.deferReply(isEphemeral()).queue();

        try {
            KarmaUserInfo karmaUserInfo = HiveBot.karmaSQLHandler.getKarmaUserInfo(sender.getIdLong());

            EmbedBuilder builder = new EmbedBuilder();
            builder.setColor(HiveBot.getColor(HiveBot.colorType.USER));
            builder.setTitle("Karma Info");
            builder.setThumbnail(sender.getEffectiveAvatarUrl());

            if(karmaUserInfo != null){

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, u");
                LocalDateTime dateTime = LocalDateTime.ofInstant(karmaUserInfo.getLastKarmaPoint(), ZoneId.systemDefault());


                builder.setDescription("Points are used to give others karma to help show their helpfulness.  **ONE point is earned per day** that you are active here on discord.");

                builder.appendDescription(String.format("\n\nYou can earn another point on: `%s`",formatter.format(dateTime.plus(1,ChronoUnit.DAYS))));

                builder.addField("Your Karma:", String.valueOf(karmaUserInfo.getKarma()),true);
                //builder.addBlankField(true);
                builder.addField("Your Points",String.valueOf(karmaUserInfo.getAvailable_points()),true);

            } else {
                builder.setDescription("Sorry,\n\nLooks like your not in our database yet.  Help others to earn some karma!");
            }

            reply(event,builder.build(),isEphemeral());

            builder.clear();


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getDescription() {
        return "Get your current karma and points.";
    }

    @Override
    public boolean isEphemeral() {
        return true;
    }
}
