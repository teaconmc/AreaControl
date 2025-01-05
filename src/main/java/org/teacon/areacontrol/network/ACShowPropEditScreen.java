package org.teacon.areacontrol.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.api.AreaProperties;
import org.teacon.areacontrol.client.EditPropertiesScreen;

import java.util.ArrayList;
import java.util.List;

public record ACShowPropEditScreen(String areaName, List<Info> props) implements CustomPacketPayload {
    public ACShowPropEditScreen(Area area, boolean isWildness) {
        this(isWildness ? null : area.name, new ArrayList<>());

        for (var prop : AreaProperties.KNOWN_PROPERTIES) {
            var maybeBool = AreaProperties.getBoolOptional(area, prop, false);
            this.props.add(new Info(prop, maybeBool.orElse(null)));
        }
    }

    public record Info(String prop, Boolean triStateValue) {
    }

    public static final CustomPacketPayload.Type<ACShowPropEditScreen> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("area_control", "show_prop_edit_screen"));

    public static final StreamCodec<FriendlyByteBuf, ACShowPropEditScreen> STREAM_CODEC = StreamCodec.composite(
            ACNetworking.asNullableCodecValue(ByteBufCodecs.STRING_UTF8),
            ACShowPropEditScreen::areaName,
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    Info::prop,
                    ACNetworking.asNullableCodecValue(ByteBufCodecs.BOOL),
                    Info::triStateValue,
                    Info::new
            ).apply(ByteBufCodecs.list()),
            ACShowPropEditScreen::props,
            ACShowPropEditScreen::new
    );

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> HandlerImpl.openScreen(this.areaName, this.props));
    }

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final class HandlerImpl {
        static void openScreen(String areaName, List<ACShowPropEditScreen.Info> props) {
            Minecraft.getInstance().setScreen(new EditPropertiesScreen(areaName, props));
        }
    }
}
