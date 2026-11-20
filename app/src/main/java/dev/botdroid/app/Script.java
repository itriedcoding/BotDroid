package dev.botdroid.app;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * A small scripting language for user-built commands, in the spirit of Bot
 * Designer for Discord's BDScript: plain text plus nested
 * {@code $function[argument;argument]} calls, variables and {@code $if} blocks.
 *
 * <p>Parsing and evaluation are done here; every side effect goes through
 * {@link Host}, so the language stays free of Android classes and can be
 * exercised on the JVM.
 */
final class Script {

    /** Everything the interpreter needs from the surrounding bot. */
    interface Host {
        String var(String key);

        void setVar(String key, String value);

        void message(String text);

        boolean hasPermission(String permission);

        /** username, displayname, userid, mention, servername, serverid, channelname,
         *  channelid, commandname, botname, version, ping, uptime, guilds, membercount. */
        String info(String key);

        String arg(int index);

        String args();

        int argCount();
    }

    static final class Result {
        final String output;
        final List<String> messages;
        final String error;

        Result(String output, List<String> messages, String error) {
            this.output = output;
            this.messages = messages;
            this.error = error;
        }

        boolean failed() {
            return error != null;
        }
    }

    static final int MAX_OUTPUT = 1900;
    private static final int MAX_DEPTH = 24;
    private static final int MAX_REPEAT = 40;
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.US);

    private Script() {
    }

    static Result run(String source, Host host) {
        Env env = new Env(host, new ArrayList<String>(), 0);
        if (source != null && !source.isEmpty()) {
            try {
                eval(source, env);
            } catch (Exception e) {
                env.error = e.getMessage() == null ? e.toString() : e.getMessage();
            }
        }
        String output = env.out.toString().trim();
        if (output.length() > MAX_OUTPUT) {
            output = output.substring(0, MAX_OUTPUT) + "…";
        }
        return new Result(output, env.messages, env.error);
    }

    /** Returns a parser error, or null when the source parses cleanly. */
    static String validate(String source) {
        Env env = new Env(new Host() {
            @Override
            public String var(String key) {
                return "";
            }

            @Override
            public void setVar(String key, String value) {
            }

            @Override
            public void message(String text) {
            }

            @Override
            public boolean hasPermission(String permission) {
                return false;
            }

            @Override
            public String info(String key) {
                return "";
            }

            @Override
            public String arg(int index) {
                return "";
            }

            @Override
            public String args() {
                return "";
            }

            @Override
            public int argCount() {
                return 0;
            }
        }, new ArrayList<String>(), 0);
        try {
            eval(source == null ? "" : source, env);
        } catch (Exception e) {
            return e.getMessage() == null ? e.toString() : e.getMessage();
        }
        return env.error;
    }

    // ------------------------------------------------------------- evaluation

    private static final class Env {
        final Host host;
        final List<String> messages;
        final StringBuilder out = new StringBuilder();
        final int depth;
        String error;

        Env(Host host, List<String> messages, int depth) {
            this.host = host;
            this.messages = messages;
            this.depth = depth;
        }

        Env child() {
            return new Env(host, messages, depth + 1);
        }
    }

    private static void eval(String text, Env env) {
        if (env.depth > MAX_DEPTH) {
            env.error = "Functions are nested too deeply (maximum " + MAX_DEPTH + ").";
            return;
        }
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                char next = text.charAt(i + 1);
                if (next == '$' || next == ';' || next == '{' || next == '}' || next == '[' || next == ']') {
                    env.out.append(next);
                    i += 2;
                    continue;
                }
                env.out.append(c);
                i++;
                continue;
            }
            if (c == '$') {
                int next = call(text, i, env);
                if (next > i) {
                    i = next;
                    continue;
                }
            }
            env.out.append(c);
            i++;
        }
    }

    /** Evaluates a nested argument and returns its text. */
    private static String evalToString(String text, Env parent) {
        Env child = parent.child();
        eval(text, child);
        if (child.error != null && parent.error == null) {
            parent.error = child.error;
        }
        return child.out.toString();
    }

    /** Consumes one $call at {@code start}; returns the index after it, or -1. */
    private static int call(String text, int start, Env env) {
        int i = start + 1;
        int nameStart = i;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                i++;
            } else {
                break;
            }
        }
        if (i == nameStart) {
            return -1;
        }
        String name = text.substring(nameStart, i).toLowerCase(Locale.US);
        String rawArgs = null;
        if (i < text.length() && text.charAt(i) == '[') {
            int close = match(text, i, '[', ']');
            if (close < 0) {
                env.error = "Missing `]` after `$" + name + "`.";
                return i;
            }
            rawArgs = text.substring(i + 1, close);
            i = close + 1;
        }

        int blockStart = skipSpace(text, i);
        if (isBlock(name) && blockStart < text.length() && text.charAt(blockStart) == '{') {
            int blockEnd = match(text, blockStart, '{', '}');
            if (blockEnd < 0) {
                env.error = "Missing `}` after `$" + name + "`.";
                return blockStart;
            }
            String body = text.substring(blockStart + 1, blockEnd);
            int after = blockEnd + 1;
            String elseBody = null;
            int elseStart = skipSpace(text, after);
            if (text.startsWith("$else", elseStart)) {
                int open = skipSpace(text, elseStart + 5);
                if (open < text.length() && text.charAt(open) == '{') {
                    int elseEnd = match(text, open, '{', '}');
                    if (elseEnd < 0) {
                        env.error = "Missing `}` after `$else`.";
                        return open;
                    }
                    elseBody = text.substring(open + 1, elseEnd);
                    after = elseEnd + 1;
                }
            }
            int end = skipSpace(text, after);
            if (text.startsWith("$end", end)) {
                after = skipSpace(text, end + 4);
            }
            runBlock(name, rawArgs == null ? "" : rawArgs, body, elseBody, env);
            return after;
        }

        List<String> values = new ArrayList<>();
        if (rawArgs != null) {
            for (String arg : splitArgs(rawArgs)) {
                values.add(evalToString(arg, env));
            }
        }
        String result = apply(name, values, env);
        env.out.append(result == null ? "" : result);
        return i;
    }

    private static boolean isBlock(String name) {
        return "if".equals(name) || "onlyif".equals(name) || "onlyperms".equals(name);
    }

    private static void runBlock(String name, String conditionRaw, String body, String elseBody, Env env) {
        String condition = evalToString(conditionRaw, env);
        boolean pass;
        if ("onlyperms".equals(name)) {
            pass = env.host.hasPermission(condition.trim());
        } else {
            pass = truthy(condition);
        }
        if (pass) {
            eval(body, env);
        } else if (elseBody != null) {
            eval(elseBody, env);
        }
    }

    // -------------------------------------------------------------- functions

    private static String apply(String name, List<String> args, Env env) {
        String a0 = arg(args, 0);
        String a1 = arg(args, 1);
        String a2 = arg(args, 2);
        switch (name) {
            case "nomention":
                return "";
            case "message":
            case "send":
                if (!a0.trim().isEmpty()) {
                    env.messages.add(a0);
                }
                return "";
            case "username":
            case "displayname":
            case "userid":
            case "mention":
            case "servername":
            case "serverid":
            case "channelname":
            case "channelid":
            case "commandname":
            case "botname":
            case "version":
            case "ping":
            case "uptime":
            case "guilds":
            case "membercount":
                return notNull(env.host.info(name));
            case "args":
                return notNull(env.host.args());
            case "argcount":
                return Integer.toString(env.host.argCount());
            case "arg":
            case "argsfrom":
                return argFrom(name, args, env);
            case "upper":
                return a0.toUpperCase(Locale.US);
            case "lower":
                return a0.toLowerCase(Locale.US);
            case "capitalize":
                return capitalize(a0);
            case "trim":
                return a0.trim();
            case "reverse":
                return new StringBuilder(a0).reverse().toString();
            case "length":
                return Integer.toString(a0.length());
            case "replace":
                return a1.isEmpty() ? a0 : a0.replace(a1, a2);
            case "substring": {
                int start = clamp(toInt(a1, 0), 0, a0.length());
                int end = a2.isEmpty() ? a0.length() : clamp(toInt(a2, a0.length()), start, a0.length());
                return a0.substring(start, end);
            }
            case "split": {
                String[] parts = a1.isEmpty() ? new String[]{a0} : a0.split(java.util.regex.Pattern.quote(a1), -1);
                int index = toInt(a2, 1) - 1;
                return index >= 0 && index < parts.length ? parts[index] : "";
            }
            case "contains":
                return bool(a0.contains(a1));
            case "startswith":
                return bool(a0.toLowerCase(Locale.US).startsWith(a1.toLowerCase(Locale.US)));
            case "endswith":
                return bool(a0.toLowerCase(Locale.US).endsWith(a1.toLowerCase(Locale.US)));
            case "repeat": {
                int count = clamp(toInt(a1, 1), 0, MAX_REPEAT);
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < count; i++) {
                    builder.append(a0);
                }
                return builder.toString();
            }
            case "random": {
                int min = toInt(a0, 1);
                int max = toInt(a1, 100);
                if (min > max) {
                    int swap = min;
                    min = max;
                    max = swap;
                }
                long span = (long) max - (long) min + 1L;
                if (span <= 0 || span > 1000000000L) {
                    env.error = "$random needs a range under a billion.";
                    return "0";
                }
                return Long.toString(min + (long) (RANDOM.nextDouble() * span));
            }
            case "choose": {
                if (args.isEmpty()) {
                    return "";
                }
                return args.get(RANDOM.nextInt(args.size()));
            }
            case "calc": {
                Double value = CommandsUtil.evaluate(a0);
                if (value == null) {
                    env.error = "`" + a0 + "` is not a sum $calc can work out.";
                    return "";
                }
                return CommandsUtil.format(value);
            }
            case "round":
                return num(Math.rint(toDouble(a0, 0)));
            case "floor":
                return num(Math.floor(toDouble(a0, 0)));
            case "ceil":
                return num(Math.ceil(toDouble(a0, 0)));
            case "abs":
                return num(Math.abs(toDouble(a0, 0)));
            case "sum":
                return num(toDouble(a0, 0) + toDouble(a1, 0));
            case "mod": {
                double divisor = toDouble(a1, 0);
                return divisor == 0 ? "0" : num(toDouble(a0, 0) % divisor);
            }
            case "max":
                return num(Math.max(toDouble(a0, 0), toDouble(a1, 0)));
            case "min":
                return num(Math.min(toDouble(a0, 0), toDouble(a1, 0)));
            case "getvar":
                return notNull(env.host.var("global::" + a0.trim()));
            case "setvar":
                env.host.setVar("global::" + a0.trim(), a1);
                return "";
            case "getuservar":
                return notNull(env.host.var("user:" + env.host.info("userid") + ":" + a0.trim()));
            case "setuservar":
                env.host.setVar("user:" + env.host.info("userid") + ":" + a0.trim(), a1);
                return "";
            case "getservervar":
                return notNull(env.host.var("server:" + env.host.info("serverid") + ":" + a0.trim()));
            case "setservervar":
                env.host.setVar("server:" + env.host.info("serverid") + ":" + a0.trim(), a1);
                return "";
            case "date":
                return LocalDate.now().format(DATE);
            case "time":
                return LocalTime.now(ZoneId.systemDefault()).format(TIME);
            case "timestamp":
                return Long.toString(System.currentTimeMillis() / 1000L);
            case "weekday": {
                LocalDate date = parseDate(a0);
                if (date == null) {
                    env.error = "`" + a0 + "` is not a date (use YYYY-MM-DD).";
                    return "";
                }
                return capitalize(date.getDayOfWeek().name().toLowerCase(Locale.US));
            }
            case "not":
                return bool(!truthy(a0));
            case "equals":
                return bool(a0.equals(a1));
            default:
                env.error = "There is no function called `$" + name + "`.";
                return "";
        }
    }

    private static String argFrom(String name, List<String> args, Env env) {
        int index = toInt(arg(args, 0), 1);
        if ("argsfrom".equals(name)) {
            StringBuilder builder = new StringBuilder();
            for (int i = index; i <= env.host.argCount(); i++) {
                if (builder.length() > 0) {
                    builder.append(' ');
                }
                builder.append(env.host.arg(i));
            }
            return builder.toString();
        }
        return notNull(env.host.arg(index));
    }

    // ----------------------------------------------------------------- helpers

    static boolean truthy(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) {
            return false;
        }
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes")) {
            return true;
        }
        if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no")) {
            return false;
        }
        String[] operators = {"==", "!=", ">=", "<=", ">", "<"};
        for (String operator : operators) {
            int index = value.indexOf(operator);
            if (index > 0) {
                String left = value.substring(0, index).trim();
                String right = value.substring(index + operator.length()).trim();
                Double leftNumber = parseNumber(left);
                Double rightNumber = parseNumber(right);
                int comparison = leftNumber != null && rightNumber != null
                        ? Double.compare(leftNumber, rightNumber)
                        : left.compareToIgnoreCase(right);
                if ("==".equals(operator)) {
                    return comparison == 0;
                }
                if ("!=".equals(operator)) {
                    return comparison != 0;
                }
                if (">=".equals(operator)) {
                    return comparison >= 0;
                }
                if ("<=".equals(operator)) {
                    return comparison <= 0;
                }
                if (">".equals(operator)) {
                    return comparison > 0;
                }
                return comparison < 0;
            }
        }
        return !"0".equals(value);
    }

    private static Double parseNumber(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static int match(String text, int start, char open, char close) {
        int depth = 0;
        boolean quoted = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            if (c == '"') {
                quoted = !quoted;
                continue;
            }
            if (quoted) {
                continue;
            }
            if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static int skipSpace(String text, int from) {
        int i = from;
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\n' || text.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }

    static List<String> splitArgs(String raw) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean quoted = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '\\' && i + 1 < raw.length()) {
                char next = raw.charAt(i + 1);
                if (next == ';' || next == '[' || next == ']' || next == '{' || next == '}' || next == '$'
                        || next == '\\') {
                    current.append(next);
                    i++;
                    continue;
                }
            }
            if (c == '"') {
                quoted = !quoted;
                continue;
            }
            if (!quoted && c == '[') {
                depth++;
            } else if (!quoted && c == ']') {
                depth--;
            } else if (!quoted && depth == 0 && c == ';') {
                parts.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        parts.add(current.toString());
        return parts;
    }

    private static String arg(List<String> args, int index) {
        return index >= 0 && index < args.size() && args.get(index) != null ? args.get(index) : "";
    }

    private static String notNull(String value) {
        return value == null ? "" : value;
    }

    private static String bool(boolean value) {
        return value ? "true" : "false";
    }

    private static String num(double value) {
        return CommandsUtil.format(value);
    }

    private static int toInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static double toDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.US) + value.substring(1);
    }

    /** Function reference shown in the bot builder and by /custom help. */
    static String reference() {
        return "**Text**\n"
                + "`$nomention` `$message[text]`\n"
                + "`$upper[x]` `$lower[x]` `$capitalize[x]` `$trim[x]` `$reverse[x]` `$length[x]`\n"
                + "`$replace[x;from;to]` `$substring[x;start;end]` `$split[x;sep;index]`\n"
                + "`$contains[x;y]` `$startsWith[x;y]` `$endsWith[x;y]` `$repeat[x;times]`\n"
                + "**Numbers**\n"
                + "`$calc[expr]` `$sum[a;b]` `$mod[a;b]` `$round[x]` `$floor[x]` `$ceil[x]` `$abs[x]`\n"
                + "`$max[a;b]` `$min[a;b]` `$random[min;max]` `$choose[a;b;c]`\n"
                + "**Context**\n"
                + "`$username` `$displayname` `$userid` `$mention` `$args` `$argcount` `$arg[n]` `$argsFrom[n]`\n"
                + "`$servername` `$serverid` `$channelname` `$channelid` `$commandname` `$botname` `$version`\n"
                + "`$ping` `$uptime` `$guilds` `$membercount` `$date` `$time` `$timestamp` `$weekday[YYYY-MM-DD]`\n"
                + "**Variables**\n"
                + "`$getVar[name]` `$setVar[name;value]` (global)\n"
                + "`$getUserVar[name]` `$setUserVar[name;value]` (per member)\n"
                + "`$getServerVar[name]` `$setServerVar[name;value]` (per server)\n"
                + "**Logic**\n"
                + "`$if[value == other]{yes}` else `$else{no}` — also `!=`, `>`, `<`, `>=`, `<=`\n"
                + "`$onlyIf[value == other]{...}` `$onlyPerms[manage_messages]{...}` `$not[x]` `$equals[x;y]`\n"
                + "Escape `$`, `;`, `{`, `}` and `[` with a backslash: `\\$5`.";
    }
}
