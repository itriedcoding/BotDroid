package dev.botdroid.app;

import org.json.JSONObject;

import java.util.Locale;
import java.util.Random;

/** Social and text fun commands. */
final class CommandsFun2 {

    private static final Random RANDOM = new Random();

    private static final String[] FORTUNES = {
            "A clever idea will visit you before the day is over.",
            "The bug you are hunting is hiding in the code you did not write.",
            "Good things come to those who push to main on a Friday. Allegedly.",
            "You will soon receive a notification you actually wanted.",
            "Patience now saves an hour of debugging later.",
            "Someone will laugh at your joke today. Possibly politely.",
            "Your next idea is better than your last one.",
            "A small refactor will repay you ten times over.",
            "The feature you fear is smaller than its ticket suggests.",
            "Luck favours the one who reads the error message.",
            "You will find the missing semicolon on the third read.",
            "A quiet evening and a good playlist are in your future."
    };

    private static final String[] COMPLIMENTS = {
            "has excellent taste in Discord bots.",
            "writes commit messages that other people actually understand.",
            "is the kind of person who reads the docs first.",
            "brings good energy to every channel.",
            "would survive a zombie apocalypse with a laptop and a hotspot.",
            "makes the server a better place just by showing up.",
            "has a suspiciously good sense of humour.",
            "is probably the reason the group chat is still alive.",
            "deserves a promotion, or at least a snack.",
            "asks the questions everyone else was afraid to ask."
    };

    private CommandsFun2() {
    }

    static void fortune(DiscordRest rest, JSONObject interaction) throws Exception {
        Commands.respond(rest, interaction, "🥠 " + FORTUNES[RANDOM.nextInt(FORTUNES.length)], false, null);
    }

    static void compliment(DiscordRest rest, JSONObject interaction) throws Exception {
        String userId = Commands.optString(interaction, "user");
        String target = userId == null ? "You" : "<@" + userId + ">";
        if (userId == null) {
            Commands.respond(rest, interaction, "**You** " + COMPLIMENTS[RANDOM.nextInt(COMPLIMENTS.length)], false,
                    null);
        } else {
            Commands.respond(rest, interaction, target + " " + COMPLIMENTS[RANDOM.nextInt(COMPLIMENTS.length)],
                    false, null);
        }
    }

    static void hug(DiscordRest rest, JSONObject interaction) throws Exception {
        action(rest, interaction, "🤗 hugs");
    }

    static void pat(DiscordRest rest, JSONObject interaction) throws Exception {
        action(rest, interaction, "👋 pats");
    }

    private static void action(DiscordRest rest, JSONObject interaction, String verb) throws Exception {
        String userId = Commands.optString(interaction, "user");
        if (userId == null) {
            Commands.respond(rest, interaction, "Pick someone first.", true, null);
            return;
        }
        String actor = Commands.invokerId(interaction);
        Commands.respond(rest, interaction, (actor == null ? "Someone" : "<@" + actor + ">") + " " + verb + " <@"
                + userId + ">!", false, null);
    }

    static void clap(DiscordRest rest, JSONObject interaction) throws Exception {
        String text = Commands.optString(interaction, "text");
        if (text == null || text.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me some text.", true, null);
            return;
        }
        String[] words = text.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                builder.append(" 👏 ");
            }
            builder.append(words[i]);
        }
        Commands.respond(rest, interaction, builder.toString(), false, null);
    }

    static void spoiler(DiscordRest rest, JSONObject interaction) throws Exception {
        String text = Commands.optString(interaction, "text");
        if (text == null || text.trim().isEmpty()) {
            Commands.respond(rest, interaction, "Give me some text to hide.", true, null);
            return;
        }
        String trimmed = text.trim();
        if (trimmed.length() > 900) {
            trimmed = trimmed.substring(0, 900);
        }
        Commands.respond(rest, interaction, "||" + trimmed + "||", false, null);
    }

    static String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.US);
    }
}
