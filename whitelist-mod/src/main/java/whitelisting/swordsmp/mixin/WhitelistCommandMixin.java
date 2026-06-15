package whitelisting.swordsmp.mixin;

import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import whitelisting.swordsmp.whitelist.WhitelistCommandGuard;
import whitelisting.swordsmp.discord.DiscordBotManager;
import whitelisting.swordsmp.linking.PlayerLinkingManager;
import java.util.Collection;
import java.util.UUID;

@Mixin(WhitelistCommand.class)
public class WhitelistCommandMixin {

    @Inject(method = "addPlayers", at = @At("HEAD"), cancellable = true)
    private static void onAddPlayers(CommandSourceStack source, Collection<NameAndId> targets, CallbackInfoReturnable<Integer> cir) {
        if (WhitelistCommandGuard.isIgnoringAdd()) return;
        for (NameAndId entry : targets) {
            String name = entry.name();
            UUID uuid = entry.id();
            String uuidStr = uuid != null ? uuid.toString() : UUID.nameUUIDFromBytes(name.toLowerCase().getBytes()).toString();
            DiscordBotManager.onInGameWhitelistAdd(name, uuidStr);

            PlayerLinkingManager pm = PlayerLinkingManager.getInstance();
            var existing = pm.getLinkByName(name);
            if (existing.isEmpty()) {
                pm.addLink("none", uuidStr, name, true);
            }
        }
    }

    @Inject(method = "removePlayers", at = @At("HEAD"), cancellable = true)
    private static void onRemovePlayers(CommandSourceStack source, Collection<NameAndId> targets, CallbackInfoReturnable<Integer> cir) {
        if (WhitelistCommandGuard.isIgnoringRemove()) return;
        for (NameAndId entry : targets) {
            String name = entry.name();
            UUID uuid = entry.id();
            String uuidStr = uuid != null ? uuid.toString() : UUID.nameUUIDFromBytes(name.toLowerCase().getBytes()).toString();
            DiscordBotManager.onInGameWhitelistRemove(name, uuidStr);

            PlayerLinkingManager pm = PlayerLinkingManager.getInstance();
            var linkOpt = pm.getLinkByName(name);
            if (linkOpt.isPresent()) {
                pm.removeLink(linkOpt.get().getMinecraftUuid());
            }
        }
    }
}
