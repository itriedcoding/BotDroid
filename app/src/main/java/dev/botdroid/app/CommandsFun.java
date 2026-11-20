package dev.botdroid.app;

import org.json.JSONObject;

import java.util.Locale;
import java.util.Random;

/** Small fun commands that need no external services. */
final class CommandsFun {

    private static final Random RANDOM = new Random();

    private static final String[] JOKES = {
            "Why do programmers prefer dark mode? Because light attracts bugs.",
            "A SQL query walks into a bar, walks up to two tables and asks: can I join you?",
            "There are 10 kinds of people: those who understand binary and those who do not.",
            "I would tell you a UDP joke, but you might not get it.",
            "Why did the developer go broke? Because he used up all his cache.",
            "It works on my machine — then we ship your machine.",
            "A byte walks into a bar, looks around and says: I think I have lost a bit.",
            "Why was the JavaScript developer sad? Because he did not know how to null his feelings.",
            "Debugging: being the detective in a crime movie where you are also the murderer.",
            "My code does not have bugs, it has randomly generated features.",
            "Why do Java developers wear glasses? Because they do not C sharp.",
            "I changed my password to incorrect, so when I forget it the computer tells me."
    };

    private static final String[] FACTS = {
            "A group of flamingos is called a flamboyance.",
            "Honey never spoils; edible jars have been found in ancient tombs.",
            "Octopuses have three hearts and blue blood.",
            "The first computer bug was a real moth found in a relay in 1947.",
            "Bananas are berries, but strawberries are not.",
            "A day on Venus is longer than a year on Venus.",
            "Sharks existed before trees.",
            "The Eiffel Tower can grow about 15 cm taller in summer heat.",
            "Wombat droppings are cube shaped.",
            "There are more possible chess games than atoms in the observable universe.",
            "The word robot comes from the Czech word for forced labour.",
            "Sea otters hold hands while sleeping so they do not drift apart."
    };

    private static final String[] QUOTES = {
            "Simplicity is the soul of efficiency. — Austin Freeman",
            "First, solve the problem. Then write the code. — John Johnson",
            "Programs must be written for people to read. — Harold Abelson",
            "Make it work, make it right, make it fast. — Kent Beck",
            "The best error message is the one that never shows up. — Thomas Fuchs",
            "Any fool can write code that a computer understands. — Martin Fowler",
            "Talk is cheap. Show me the code. — Linus Torvalds",
            "Premature optimization is the root of all evil. — Donald Knuth",
            "It always seems impossible until it is done. — Nelson Mandela",
            "Do not watch the clock. Do what it does: keep going. — Sam Levenson",
            "The only way to learn a new programming language is by writing programs in it. — Dennis Ritchie",
            "Code is like humour. When you have to explain it, it is bad. — Cory House"
    };

    private CommandsFun() {
    }

    static void coinFlip(DiscordRest rest, JSONObject interaction) throws Exception {
        Commands.respond(rest, interaction, RANDOM.nextBoolean() ? "🪙 **Heads**" : "🪙 **Tails**", false, null);
    }

    static void rps(DiscordRest rest, JSONObject interaction) throws Exception {
        String choice = Commands.optString(interaction, "choice");
        if (choice == null) {
            Commands.respond(rest, interaction, "Pick rock, paper or scissors.", true, null);
            return;
        }
        String player = choice.toLowerCase(Locale.US);
        int playerIndex = "rock".equals(player) ? 0 : "paper".equals(player) ? 1 : 2;
        String[] moves = {"rock", "paper", "scissors"};
        int botIndex = RANDOM.nextInt(3);
        String result;
        if (playerIndex == botIndex) {
            result = "It is a draw.";
        } else if ((playerIndex - botIndex + 3) % 3 == 1) {
            result = "You win!";
        } else {
            result = "I win!";
        }
        Commands.respond(rest, interaction, emoji(player) + " vs " + emoji(moves[botIndex]) + "\n**" + result + "**",
                false, null);
    }

    private static String emoji(String move) {
        if ("rock".equals(move)) {
            return "🪨 rock";
        }
        if ("paper".equals(move)) {
            return "📄 paper";
        }
        return "✂️ scissors";
    }

    static void joke(DiscordRest rest, JSONObject interaction) throws Exception {
        Commands.respond(rest, interaction, JOKES[RANDOM.nextInt(JOKES.length)], false, null);
    }

    static void fact(DiscordRest rest, JSONObject interaction) throws Exception {
        Commands.respond(rest, interaction, "💡 " + FACTS[RANDOM.nextInt(FACTS.length)], false, null);
    }

    static void quote(DiscordRest rest, JSONObject interaction) throws Exception {
        Commands.respond(rest, interaction, "> " + QUOTES[RANDOM.nextInt(QUOTES.length)], false, null);
    }

    static void rate(DiscordRest rest, JSONObject interaction) throws Exception {
        String thing = Commands.optString(interaction, "thing");
        if (thing == null || thing.trim().isEmpty()) {
            Commands.respond(rest, interaction, "What should I rate?", true, null);
            return;
        }
        int score = Math.abs(thing.trim().toLowerCase(Locale.US).hashCode()) % 101;
        StringBuilder bar = new StringBuilder();
        int filled = score / 10;
        for (int i = 0; i < 10; i++) {
            bar.append(i < filled ? '█' : '░');
        }
        Commands.respond(rest, interaction, "**" + thing.trim() + "** — " + score + "/100\n" + bar, false, null);
    }

    static void reverse(DiscordRest rest, JSONObject interaction) throws Exception {
        String text = Commands.optString(interaction, "text");
        if (text == null || text.isEmpty()) {
            Commands.respond(rest, interaction, "Give me some text to reverse.", true, null);
            return;
        }
        Commands.respond(rest, interaction, new StringBuilder(text).reverse().toString(), false, null);
    }
}
