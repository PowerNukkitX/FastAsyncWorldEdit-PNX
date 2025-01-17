/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sk89q.worldedit.pnx;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockCommandBlock;
import cn.nukkit.blockentity.BlockEntityCommandBlock;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extension.platform.AbstractNonPlayerActor;
import com.sk89q.worldedit.extension.platform.Locatable;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.session.SessionKey;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.util.auth.AuthorizationException;
import com.sk89q.worldedit.util.formatting.WorldEditText;
import com.sk89q.worldedit.util.formatting.text.Component;
import com.sk89q.worldedit.util.formatting.text.TextComponent;
import com.sk89q.worldedit.util.formatting.text.format.TextColor;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

public class PNXBlockCommandSender extends AbstractNonPlayerActor implements Locatable {

    private static final String UUID_PREFIX = "CMD";

    private final BlockEntityCommandBlock sender;
    private final PNXWorldEditPlugin plugin;
    private final Location location;
    private final UUID uuid;

    public PNXBlockCommandSender(PNXWorldEditPlugin plugin, BlockEntityCommandBlock sender) {
        checkNotNull(plugin);
        checkNotNull(sender);

        this.plugin = plugin;
        this.sender = sender;
        this.location = PNXAdapter.adapt(sender.getBlock().getLocation());
        this.uuid = UUID.nameUUIDFromBytes((UUID_PREFIX + sender.getName()).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getName() {
        return sender.getName();
    }

    @Override
    @Deprecated
    public void printRaw(String msg) {
        //FAWE start - ensure executed on main thread
        TaskManager.taskManager().sync(() -> {
            for (String part : msg.split("\n")) {
                sender.sendMessage(part);
            }
            return null;
        });
        //FAWE end
    }

    @Override
    @Deprecated
    public void print(String msg) {
        //FAWE start - ensure executed on main thread
        TaskManager.taskManager().sync(() -> {
            for (String part : msg.split("\n")) {
                print(TextComponent.of(part, TextColor.LIGHT_PURPLE));
            }
            return null;
        });
        //FAWE end
    }

    @Override
    @Deprecated
    public void printDebug(String msg) {
        //FAWE start - ensure executed on main thread
        TaskManager.taskManager().sync(() -> {
            for (String part : msg.split("\n")) {
                print(TextComponent.of(part, TextColor.GRAY));
            }
            return null;
        });
        //FAWE end
    }

    @Override
    @Deprecated
    public void printError(String msg) {
        //FAWE start - ensure executed on main thread
        TaskManager.taskManager().sync(() -> {
            for (String part : msg.split("\n")) {
                print(TextComponent.of(part, TextColor.RED));
            }
            return null;
        });
        //FAWE end
    }

    @Override
    public void print(final Component component) {
        sender.sendMessage(WorldEditText.format(component, getLocale()).toString());
    }

//    @Override
//    public void print(Component component) {
//        //FAWE start - ensure executed on main thread
//        TaskManager.taskManager().sync(() -> {
//            TextAdapter.sendMessage(sender, WorldEditText.format(component, getLocale()));
//            return null;
//        });
//        //FAWE end
//    }

    @Override
    public Locale getLocale() {
        return WorldEdit.getInstance().getConfiguration().defaultLocale;
    }

    @Override
    public Location getLocation() {
        return this.location;
    }

    @Override
    public boolean setLocation(Location location) {
        return false;
    }

    @Override
    public Extent getExtent() {
        return this.location.getExtent();
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public String[] getGroups() {
        return new String[0];
    }

    @Override
    public void checkPermission(String permission) throws AuthorizationException {
        if (!hasPermission(permission)) {
            throw new AuthorizationException();
        }
    }

    @Override
    public boolean hasPermission(String permission) {
        return sender.hasPermission(permission);
    }

    //FAWE start
    @Override
    public boolean togglePermission(String permission) {
        return true;
    }
    //FAWE end

    @Override
    public void setPermission(String permission, boolean value) {
    }

    public BlockEntityCommandBlock getSender() {
        return this.sender;
    }

    @Override
    public SessionKey getSessionKey() {
        return new SessionKey() {

            private volatile boolean active = true;

            private void updateActive() {
                Block block = sender.getBlock();
                if (!block.getLevel().isChunkLoaded(block.getChunkX(), block.getChunkZ())) {
                    active = false;
                    return;
                }
                active = block instanceof BlockCommandBlock;
            }

            @Override
            public String getName() {
                return sender.getName();
            }

            @Override
            public boolean isActive() {
                if (Server.getInstance().isPrimaryThread()) {
                    // we can update eagerly
                    updateActive();
                } else {
                    // we should update it eventually
                    Server.getInstance().getScheduler().scheduleTask(
                            plugin,
                            this::updateActive
                    );
                }
                return active;
            }

            @Override
            public boolean isPersistent() {
                return true;
            }

            @Override
            public UUID getUniqueId() {
                return uuid;
            }
        };
    }

}
