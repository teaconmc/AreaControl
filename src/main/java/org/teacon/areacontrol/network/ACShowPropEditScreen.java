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
    public ACShowPropEditScreen(Area area) {
        this(area.name, new ArrayList<>());

        for (var prop : AreaProperties.KNOWN_PROPERTIES) {
            var maybeBool = AreaProperties.getBoolOptional(area, prop, false);
            this.props.add(new Info(prop, maybeBool.orElse(null)));
        }
    }

    public record Info(String prop, Boolean triStateValue) {
    }

    public static final CustomPacketPayload.Type<ACShowPropEditScreen> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("area_control", "show_prop_edit_screen"));

    public static final StreamCodec<FriendlyByteBuf, ACShowPropEditScreen> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ACShowPropEditScreen::areaName,
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    Info::prop,
                    new StreamCodec<FriendlyByteBuf, Boolean>() {
                        private static final byte V_NULL = 1, V_TRUE = 2, V_FALSE = 3;

                        @Override
                        public void encode(@NotNull FriendlyByteBuf buf, @NotNull Boolean value) {
                            if (value == null) {
                                buf.writeByte(V_NULL);
                            } else {
                                buf.writeShort(value ? V_TRUE : V_FALSE);
                            }
                        }

                        @Override
                        @NotNull
                        public Boolean decode(@NotNull FriendlyByteBuf buf) {
                            return switch (buf.readByte()) {
                                case V_NULL -> null;
                                case V_TRUE -> Boolean.TRUE;
                                case V_FALSE -> Boolean.FALSE;
                                default -> throw new IllegalArgumentException("Invalid nullable boolean value.");
                            };
                        }
                    },
                    Info::triStateValue,
                    Info::new
            ).apply(ByteBufCodecs.list()),
            ACShowPropEditScreen::props,
            ACShowPropEditScreen::new
    );

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> HandlerImpl.openScreen0(this.areaName, this.props));
    }

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static final class HandlerImpl {
        static void openScreen0(String areaName, List<ACShowPropEditScreen.Info> props) {
            Minecraft.getInstance().setScreen(new EditPropertiesScreen(areaName, props));
        }
    }
}
