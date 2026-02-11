package combat.log.discord.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for the Discord bot
 */
public class BotConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // Discord settings - Essential credentials
    public DiscordSettings discord = new DiscordSettings();
    
    // WebSocket settings
    public WebSocketSettings websocket = new WebSocketSettings();
    
    // Feature toggles
    public FeatureSettings features = new FeatureSettings();
    
    // Timeout settings
    public TimeoutSettings timeouts = new TimeoutSettings();
    
    // Channel IDs
    public ChannelSettings channels = new ChannelSettings();
    
    // Ticket-specific settings
    public TicketSettings ticket = new TicketSettings();
    
    // Whitelist-specific settings
    public WhitelistSettings whitelist = new WhitelistSettings();

    // Button label settings
    public ButtonSettings buttons = new ButtonSettings();

    // Message templates
    public Map<String, String> messages = new HashMap<>();

    public BotConfig() {
        applyMessageDefaults();
    }

    public static class DiscordSettings {
        public String token = "YOUR_BOT_TOKEN_HERE";
        public String guildId = "YOUR_GUILD_ID";
        public String staffRoleId = "YOUR_STAFF_ROLE_ID";
    }

    public static class WebSocketSettings {
        public int port = 8080;
        public String host = "0.0.0.0";
        public String authToken = ""; // Token expected from connecting mods
    }
    
    public static class FeatureSettings {
        public boolean useForumChannel = true;
        public boolean autoDenyEnabled = true;
        public boolean privateThreads = true;
        public boolean whitelistEnabled = true;
        public boolean mojangApiEnabled = true;
    }
    
    public static class TimeoutSettings {
        public long ticketTimeoutMinutes = 60;
        public long mojangCacheDurationMinutes = 5;
        public int mojangApiTimeoutSeconds = 5;
    }
    
    public static class ChannelSettings {
        public String ticketChannelId = "YOUR_CHANNEL_ID";
        public String whitelistChannelId = "YOUR_WHITELIST_CHANNEL_ID";
        public String reviewChannelId = "YOUR_REVIEW_CHANNEL_ID";
        public String whitelistLogChannelId = "YOUR_WHITELIST_LOG_CHANNEL_ID";
    }

    public static class TicketSettings {
        public String[] acceptedProofPlatforms = {
            "youtube.com", "youtu.be",
            "twitch.tv", "clips.twitch.tv",
            "streamable.com",
            "medal.tv",
            "discord.com/attachments"
        };
    }
    
    public static class WhitelistSettings {
        public ButtonMessageSettings buttonMessage = new ButtonMessageSettings();
    }
    
    public static class ButtonMessageSettings {
        public String title = "🎫 Request Server Whitelist";
        public String description = "Click the button below to request access to our Minecraft server";
        public String color = "#00FF00";
    }

    public static class ButtonSettings {
        public TicketButtonSettings ticket = new TicketButtonSettings();
        public WhitelistButtonSettings whitelist = new WhitelistButtonSettings();
    }

    public static class TicketButtonSettings {
        public String approve = "✅ Approve";
        public String deny = "❌ Deny";
        public String admit = "🔴 I Admit Combat Log";
        public String extend = "⏰ Extend";
    }

    public static class WhitelistButtonSettings {
        public String request = "🎫 Request Whitelist";
        public String approve = "✅ Approve";
        public String deny = "❌ Deny";
        public String unlink = "🔓 Unlink";
    }

    public static BotConfig load(File configFile) {
        BotConfig baseConfig = loadFromFile(configFile);
        if (baseConfig == null) {
            baseConfig = new BotConfig();
            save(configFile, baseConfig);
        }

        File overrideFile = resolveOverrideFile(configFile);
        if (overrideFile.exists()) {
            try (FileReader reader = new FileReader(overrideFile)) {
                JsonObject baseObj = GSON.toJsonTree(baseConfig).getAsJsonObject();
                JsonObject overrideObj = JsonParser.parseReader(reader).getAsJsonObject();
                deepMerge(baseObj, overrideObj);
                baseConfig = GSON.fromJson(baseObj, BotConfig.class);
            } catch (Exception e) {
                System.err.println("Failed to load config override: " + e.getMessage());
            }
        }

        baseConfig.applyMessageDefaults();
        return baseConfig;
    }

    public static void save(File configFile, BotConfig config) {
        try {
            File parent = configFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                GSON.toJson(config, writer);
                System.out.println("Saved configuration to " + configFile.getName());
            }
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
        }
    }

    private static BotConfig loadFromFile(File configFile) {
        if (!configFile.exists()) {
            return null;
        }

        try (FileReader reader = new FileReader(configFile)) {
            return GSON.fromJson(reader, BotConfig.class);
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
            return null;
        }
    }

    private static File resolveOverrideFile(File configFile) {
        File parent = configFile.getParentFile();
        if (parent != null) {
            return new File(parent, "config.local.json");
        }
        return new File("config.local.json");
    }

    public String message(String key, String fallback) {
        if (messages == null) {
            return fallback;
        }
        return messages.getOrDefault(key, fallback);
    }

    private void applyMessageDefaults() {
        ensureMessage("whitelist.setup.channelRequired", "❌ Channel ID is required");
        ensureMessage("whitelist.setup.success", "✅ Whitelist channel setup complete!");
        ensureMessage("whitelist.setup.failure", "❌ Failed to setup whitelist channel: {error}");
        ensureMessage("whitelist.modal.usernameMissing", "❌ Error: Username not provided");
        ensureMessage("whitelist.modal.waiting", "⏳ Waiting for Minecraft confirmation...");
        ensureMessage("whitelist.modal.requestInvalidFormat", "❌ Invalid Minecraft username format. Username must be 3-16 characters long and contain only letters, numbers, and underscores.");
        ensureMessage("whitelist.modal.requestAlreadyPending", "❌ You already have a whitelist request being processed. Please wait a moment.");
        ensureMessage("whitelist.modal.requestAlreadyLinked", "❌ Your Discord account is already linked to a Minecraft account.");
        ensureMessage("whitelist.modal.requestNameNotFound", "❌ Minecraft username not found. Please check the spelling and try again.");
        ensureMessage("whitelist.modal.requestMinecraftLinked", "❌ This Minecraft account is already linked to another Discord account.");
        ensureMessage("whitelist.modal.linkError", "❌ An error occurred while linking your account. Please try again later.");
        ensureMessage("whitelist.modal.processError", "❌ An error occurred while processing your request. Please try again later.");
        ensureMessage("whitelist.confirm.success", "✅ Whitelist successful for **{playerName}**.");
        ensureMessage("whitelist.confirm.fail", "❌ Whitelist failed for **{playerName}**: {error}");
        ensureMessage("whitelist.deny.success", "❌ Request denied.");
        ensureMessage("whitelist.approve.actionMessage", "✅ Request approved! Whitelisting player...");
        ensureMessage("whitelist.unlink.notLinked", "❌ Your Discord account is not linked.");
        ensureMessage("whitelist.unlink.already", "✅ Your Discord account is already unlinked.");
        ensureMessage("whitelist.unlink.notFound", "❌ No linked Minecraft account found to unlink.");
        ensureMessage("whitelist.unlink.success", "✅ Your Discord account has been unlinked from {minecraftName}.");
        ensureMessage("whitelist.unlink.error", "❌ Failed to unlink your account. Please try again later.");
        ensureMessage("whitelist.unlink.noPermission", "❌ You don't have permission to use this command.");
        ensureMessage("whitelist.unlink.missingUser", "❌ Discord user is required.");
        ensureMessage("whitelist.unlink.adminSuccess", "✅ Unlink sent and logged.");
        ensureMessage("whitelist.unlink.adminNotFound", "❌ No linked Minecraft account found for that user.");
        ensureMessage("whitelist.link.prompt.title", "🔗 Link Discord Account");
        ensureMessage("whitelist.link.prompt.body", "Minecraft: {playerName}\nUUID: `{playerUuid}`\n\nClick **Link Discord** and enter the user's Discord mention or ID.");
        ensureMessage("whitelist.link.modal.title", "Link Discord Account");
        ensureMessage("whitelist.link.modal.label", "Discord User (mention or ID)");
        ensureMessage("whitelist.link.modal.placeholder", "@User or 1234567890");
        ensureMessage("whitelist.link.success", "✅ Linked {playerName} to <@{discordId}>.");
        ensureMessage("whitelist.link.alreadyLinked", "❌ This Minecraft account is already linked.");
        ensureMessage("whitelist.link.discordLinked", "❌ That Discord user is already linked.");
        ensureMessage("whitelist.link.invalidUser", "❌ Invalid Discord user. Please use a mention or ID.");
        ensureMessage("whitelist.notify.unlinked", "You unlinked your Discord. Re-link in Discord to rejoin.");
        ensureMessage("whitelist.notify.adminUnlinked", "A staff member unlinked your Discord. Go to Discord to re-link.");
        ensureMessage("whitelist.notify.whitelistRemoved", "Your whitelist entry was removed. Go to Discord to re-link.");
        ensureMessage("whitelist.log.success.title", "✅ Whitelist Successful");
        ensureMessage("whitelist.log.success.desc", "Discord: <@{discordId}>\nMinecraft: {playerName}");
        ensureMessage("whitelist.log.fail.title", "❌ Whitelist Failed");
        ensureMessage("whitelist.log.fail.desc", "Discord: <@{discordId}>\nMinecraft: {playerName}\nError: {error}");
        ensureMessage("whitelist.log.remove.title", "🗑️ Whitelist Removed");
        ensureMessage("whitelist.log.remove.desc", "Minecraft: {playerName}");
        ensureMessage("whitelist.modal.title", "Request Whitelist");
        ensureMessage("whitelist.modal.usernameLabel", "Minecraft Username");
        ensureMessage("whitelist.modal.usernamePlaceholder", "Enter your Minecraft username");
        ensureMessage("whitelist.deny.modal.title", "Deny Whitelist Request");
        ensureMessage("whitelist.deny.modal.reasonLabel", "Reason for Denial");
        ensureMessage("whitelist.deny.modal.reasonPlaceholder", "Enter reason for denial (optional)");

        ensureMessage("ticket.commands.unknown", "Unknown command");
        ensureMessage("ticket.commands.missingIncidentId", "❌ Please provide an incident ID");
        ensureMessage("ticket.commands.missingIncidentIdMinutes", "❌ Please provide incident ID and minutes");
        ensureMessage("ticket.commands.ticketNotFound", "❌ Ticket not found: {incidentId}");
        ensureMessage("ticket.modal.admitConfirm", "❌ You must type 'I admit' to confirm");
        ensureMessage("ticket.modal.extendRange", "❌ Please enter a valid number between 1 and 1440 minutes");
        ensureMessage("ticket.modal.invalidNumber", "❌ Invalid number format. Please enter a valid number.");

        ensureMessage("ticket.embed.approved.title", "✅ Ticket Approved");
        ensureMessage("ticket.embed.approved.desc", "Player will NOT be punished on next login.");
        ensureMessage("ticket.embed.denied.title", "❌ Ticket Denied");
        ensureMessage("ticket.embed.denied.desc", "Player WILL be killed on next login.");
        ensureMessage("ticket.embed.extended.title", "⏰ Ticket Extended");
        ensureMessage("ticket.embed.info.title", "📋 Ticket Information");
        ensureMessage("ticket.embed.admit.title", "🔴 Combat Log Admitted");
        ensureMessage("ticket.embed.admit.desc", "You admitted to combat logging. Same consequences as denial apply:\n• You WILL be killed on next login\n• Your items are in a player head at logout location\n• Opponents can access for 30 minutes\n• After 30 min, everyone can access\n\nThank you for your honesty.");
        ensureMessage("ticket.embed.admit.status", "DENIED (Self-Admitted)");

        ensureMessage("ticket.embed.field.incidentId", "Incident ID");
        ensureMessage("ticket.embed.field.approvedBy", "Approved By");
        ensureMessage("ticket.embed.field.deniedBy", "Denied By");
        ensureMessage("ticket.embed.field.admittedBy", "Admitted By");
        ensureMessage("ticket.embed.field.reason", "Reason");
        ensureMessage("ticket.embed.field.extendedBy", "Extended By");
        ensureMessage("ticket.embed.field.extendedByUser", "Extended By User");
        ensureMessage("ticket.embed.field.player", "Player");
        ensureMessage("ticket.embed.field.status", "Status");
        ensureMessage("ticket.embed.field.combatTimeRemaining", "Combat Time Remaining");
        ensureMessage("ticket.embed.field.created", "Created");
        ensureMessage("ticket.embed.field.expires", "Expires");
        ensureMessage("ticket.embed.field.clipUrl", "Clip URL");
        ensureMessage("ticket.embed.field.clipSubmitted", "Clip Submitted");
        ensureMessage("ticket.embed.field.timeRemaining", "Time Remaining");
        ensureMessage("ticket.embed.field.discordLinked", "Discord Linked");
        ensureMessage("ticket.embed.field.deadline", "Deadline");
        ensureMessage("ticket.embed.field.consequence", "Consequence");

        ensureMessage("ticket.thread.forumTitle", "🚨 Combat Log: {playerName}");
        ensureMessage("ticket.thread.textTitle", "Combat Log: {playerName}");
        ensureMessage("ticket.incident.title", "⚔️ Combat Log Report");
        ensureMessage("ticket.incident.desc", "A player has disconnected during combat and needs to provide proof.");
        ensureMessage("ticket.incident.discordLinkedYes", "✅ {userMention}");
        ensureMessage("ticket.incident.discordLinkedNo", "❌ Not linked");
        ensureMessage("ticket.incident.statusPending", "⏳ Pending Proof");
        ensureMessage("ticket.incident.consequence", "❌ Killed on next login if not resolved");
        ensureMessage("ticket.incident.footer", "Combat Log System");

        ensureMessage("ticket.staff.alert", "{staffMention} - New combat log incident!");
        ensureMessage("ticket.player.reviewForum", "{userMention} - Please review the incident and submit proof if needed.");
        ensureMessage("ticket.player.reviewThread", "{userMention} - Please review this incident.");

        ensureMessage("ticket.instructions", "**📋 Instructions:**\n\n**For {userMention}:**\n• Upload a clip/video showing you disconnected unintentionally (crash, internet issue, etc.)\n• Accepted platforms: YouTube, Twitch, Streamable, Medal.tv, or Discord upload\n• You have **{minutes} minutes** to submit proof\n• If no proof is submitted, you will be killed on your next login\n\n**For Staff:**\n• Click **{approveLabel}** to clear the punishment\n• Click **{denyLabel}** to confirm the punishment\n• Click **{extendLabel}** to give more time\n• Or use `/info <incident_id>` for details");

        ensureMessage("ticket.notification.title", "🚨 Combat Log Ticket Created");
        ensureMessage("ticket.notification.desc", "You have disconnected during combat and a ticket has been created.");
        ensureMessage("ticket.notification.field.whatHappened", "What happened?");
        ensureMessage("ticket.notification.value.whatHappened", "You disconnected while in combat with another player.");
        ensureMessage("ticket.notification.field.whatToDo", "What do I need to do?");
        ensureMessage("ticket.notification.value.whatToDo", "Submit proof (clip/video) showing you disconnected unintentionally (crash, internet issue, etc.)");
        ensureMessage("ticket.notification.field.where", "Where?");
        ensureMessage("ticket.notification.value.where", "In the ticket: {threadMention}");
        ensureMessage("ticket.notification.field.deadline", "Deadline");
        ensureMessage("ticket.notification.value.deadline", "You have **{minutes} minutes** to submit proof");
        ensureMessage("ticket.notification.field.whatIf", "What if I don't?");
        ensureMessage("ticket.notification.value.whatIf", "You will be killed when you next log into the server.");
        ensureMessage("ticket.notification.footer", "Use the thread link above to open your ticket");

        ensureMessage("ticket.statusUpdate.title", "📊 Ticket Status Update");
        ensureMessage("ticket.statusUpdate.field.status", "Status");
        ensureMessage("ticket.statusUpdate.field.details", "Details");
        ensureMessage("ticket.status.approved", "✅ Approved by {adminName}");
        ensureMessage("ticket.status.denied", "❌ Denied by {adminName}");
        ensureMessage("ticket.status.extended", "⏰ Extended by {minutes} minutes");
        ensureMessage("ticket.status.clipSubmitted", "📹 Clip submitted - Awaiting staff review");
        ensureMessage("ticket.status.autoDenied", "⏱️ Auto-Denied (Timeout)");
        ensureMessage("ticket.status.autoDeniedDetails", "No proof was submitted within the deadline");

        ensureMessage("ticket.modal.approve.title", "✅ Approve Ticket");
        ensureMessage("ticket.modal.deny.title", "❌ Deny Ticket");
        ensureMessage("ticket.modal.admit.title", "🔴 Admit Combat Logging");
        ensureMessage("ticket.modal.extend.title", "⏰ Extend Deadline");
        ensureMessage("ticket.modal.reason.label", "Reason (Optional)");
        ensureMessage("ticket.modal.reason.approvePlaceholder", "Enter reason for approval (e.g., 'Clear crash', 'Internet issue')");
        ensureMessage("ticket.modal.reason.denyPlaceholder", "Enter reason for denial (e.g., 'Combat logging', 'No valid proof')");
        ensureMessage("ticket.modal.admit.label", "Type 'I admit' to confirm");
        ensureMessage("ticket.modal.admit.placeholder", "Type: I admit");
        ensureMessage("ticket.modal.extend.label", "Additional Minutes");
        ensureMessage("ticket.modal.extend.placeholder", "Enter number of minutes (e.g., 30)");
    }

    private void ensureMessage(String key, String value) {
        if (messages == null) {
            messages = new HashMap<>();
        }
        messages.putIfAbsent(key, value);
    }

    private static void deepMerge(JsonObject target, JsonObject override) {
        for (String key : override.keySet()) {
            JsonElement overrideValue = override.get(key);
            if (overrideValue != null && overrideValue.isJsonObject()
                && target.has(key) && target.get(key).isJsonObject()) {
                deepMerge(target.getAsJsonObject(key), overrideValue.getAsJsonObject());
            } else {
                target.add(key, overrideValue);
            }
        }
    }
}
