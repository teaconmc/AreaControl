package org.teacon.areacontrol.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.client.EditPropertiesScreen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ACShowPropEditScreen {

    private final String areaName;
    private final List<Info> props = new ArrayList<>();

    public ACShowPropEditScreen(Area area) {
        this.areaName = area.name;
        for (var prop : AreaProperties.KNOWN_PROPERTIES) {
            var maybeBool = AreaProperties.getBoolOptional(area, prop, false);
            props.add(new Info(prop, maybeBool.orElse(null)));
        }
    }

    public ACShowPropEditScreen(FriendlyByteBuf buf) {
        this.areaName = buf.readUtf();
        var count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            var prop = buf.readUtf();
            if (buf.readBoolean()) {
                this.props.add(new Info(prop, buf.readBoolean()));
            } else {
                this.props.add(new Info(prop, null));
            }
        }
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(this.areaName);
        buf.writeVarInt(this.props.size());
        for (var entry : this.props) {
            buf.writeUtf(entry.prop);
            if (entry.triStateValue != null) {
                buf.writeBoolean(true).writeBoolean(entry.triStateValue);
            } else {
                buf.writeBoolean(false);
            }
        }
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        var context = contextSupplier.get();
        context.enqueueWork(() -> HandlerImpl.openScreen0(this.areaName, this.props));
        context.setPacketHandled(true);
    }

    public record Info(String prop, Boolean triStateValue) {

    }

    private static final class HandlerImpl {
        static void openScreen0(String areaName, List<ACShowPropEditScreen.Info> props) {
            Minecraft.getInstance().setScreen(new EditPropertiesScreen(areaName, props));
        }
    }
}
