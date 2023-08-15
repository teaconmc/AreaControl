package org.teacon.areacontrol.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.teacon.areacontrol.api.Area;
import org.teacon.areacontrol.network.ACNetworking;
import org.teacon.areacontrol.network.ACPingServer;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = "area_control", value = Dist.CLIENT)
public final class AreaControlClientSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger("AreaControl");
    private static final Marker MARKER = MarkerFactory.getMarker("Client");

    //@SubscribeEvent
    public static void shaderSetup(RegisterShadersEvent event) throws IOException {
        /*event.registerShader(...);*/
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        LOGGER.info(MARKER, "AreaControl is installed on client; enabling enhanced client support");

        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, ClientPlayerNetworkEvent.LoggingIn.class,
                AreaControlClientSupport::afterLogin);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, EntityJoinLevelEvent.class,
                AreaControlClientSupport::resetNearbyAreas);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, RenderLevelStageEvent.class,
                AreaControlClientSupport::renderAreaBorder);
    }

    static void resetNearbyAreas(EntityJoinLevelEvent event) {
        // If player enters a new level, reset nearby areas
        if (event.getLevel().isClientSide() && event.getEntity() instanceof Player) {
            knownAreas = Collections.emptyList();
            knownAreasExpiresAt = 0L;
        }
    }

    static void afterLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        ACNetworking.acNetworkChannel.send(PacketDistributor.SERVER.with(null), new ACPingServer());
    }

    public static volatile List<Area.Summary> knownAreas = Collections.emptyList();
    public static volatile long knownAreasExpiresAt = 0L;
    public static volatile BlockPos selectionMin, selectionMax;

    static void renderAreaBorder(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        final Minecraft mc = Minecraft.getInstance();
        final var transform = event.getPoseStack();
        final var camera = mc.getEntityRenderDispatcher().camera;
        if (camera == null) {
            // BloCamLimb said this can be null when OptiFine is installed.
            return;
        }
        transform.pushPose();
        final var proj = camera.getPosition();
        transform.translate(-proj.x, -proj.y, -proj.z);

        var buffers = mc.renderBuffers().bufferSource();

        var builder =  buffers.getBuffer(Holder.BORDER);

        var renderDistance = mc.options.getEffectiveRenderDistance() * 16;
        BlockPos playerPos;
        if (mc.player != null) {
            playerPos = mc.player.blockPosition();
        } else {
            playerPos = BlockPos.ZERO;
        }
        if (System.currentTimeMillis() < knownAreasExpiresAt) {
            for (var area : knownAreas) {
                if (playerPos.closerThan(new Vec3i(area.midX, area.midY, area.midZ), renderDistance)) {
                    int minY = Math.max(-128, area.minY);
                    int maxY = Math.min(320, area.maxY);
                    box(transform, builder, area.enclosed ? 0x8826619C : 0x887FFFD4, area.minX, minY, area.minZ, area.maxX + 1, maxY + 1, area.maxZ + 1);
                }
            }
        }
        if (selectionMin != null && selectionMax != null) {
            box(transform, builder, 0xFFFFD700, selectionMin.getX(), selectionMin.getY(), selectionMin.getZ(), selectionMax.getX() + 1, selectionMax.getY() + 1, selectionMax.getZ() + 1);
        }

        buffers.endBatch();

        transform.popPose();
    }

    static void box(PoseStack pose, VertexConsumer vertexConsumer, int argbColor, 
                    int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int diffX = maxX - minX;
        int diffY = maxY - minY;
        int diffZ = maxZ - minZ;
        var x = pose.last().pose();

        // Bottom interior
        vertexConsumer.vertex(x, minX, minY, minZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, minX, minY, maxZ).color(argbColor).uv(0, diffZ).endVertex();
        vertexConsumer.vertex(x, maxX, minY, maxZ).color(argbColor).uv(diffX, diffZ).endVertex();
        vertexConsumer.vertex(x, maxX, minY, minZ).color(argbColor).uv(diffX, 0).endVertex();

        // Bottom exterior
        vertexConsumer.vertex(x, minX, minY, minZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, maxX, minY, minZ).color(argbColor).uv(0, diffX).endVertex();
        vertexConsumer.vertex(x, maxX, minY, maxZ).color(argbColor).uv(diffZ, diffX).endVertex();
        vertexConsumer.vertex(x, minX, minY, maxZ).color(argbColor).uv(diffZ, 0).endVertex();

        // Top interior
        vertexConsumer.vertex(x, minX, maxY, minZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, minZ).color(argbColor).uv(0, diffX).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, maxZ).color(argbColor).uv(diffZ, diffX).endVertex();
        vertexConsumer.vertex(x, minX, maxY, maxZ).color(argbColor).uv(diffZ, 0).endVertex();

        // Top exterior
        vertexConsumer.vertex(x, minX, maxY, minZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, minX, maxY, maxZ).color(argbColor).uv(0, diffZ).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, maxZ).color(argbColor).uv(diffX, diffZ).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, minZ).color(argbColor).uv(diffX, 0).endVertex();

        // Front interior
        vertexConsumer.vertex(x, minX, minY, maxZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, minX, maxY, maxZ).color(argbColor).uv(0, diffY).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, maxZ).color(argbColor).uv(diffX, diffY).endVertex();
        vertexConsumer.vertex(x, maxX, minY, maxZ).color(argbColor).uv(diffX, 0).endVertex();

        // Front exterior
        vertexConsumer.vertex(x, minX, minY, maxZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, maxX, minY, maxZ).color(argbColor).uv(0, diffX).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, maxZ).color(argbColor).uv(diffY, diffX).endVertex();
        vertexConsumer.vertex(x, minX, maxY, maxZ).color(argbColor).uv(diffY, 0).endVertex();

        // Back interior
        vertexConsumer.vertex(x, minX, minY, minZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, maxX, minY, minZ).color(argbColor).uv(0, diffX).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, minZ).color(argbColor).uv(diffY, diffX).endVertex();
        vertexConsumer.vertex(x, minX, maxY, minZ).color(argbColor).uv(diffY, 0).endVertex();

        // Back exterior
        vertexConsumer.vertex(x, minX, minY, minZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, minX, maxY, minZ).color(argbColor).uv(0, diffY).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, minZ).color(argbColor).uv(diffX, diffY).endVertex();
        vertexConsumer.vertex(x, maxX, minY, minZ).color(argbColor).uv(diffX, 0).endVertex();

        // Left interior
        vertexConsumer.vertex(x, minX, minY, minZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, minX, maxY, minZ).color(argbColor).uv(0, diffY).endVertex();
        vertexConsumer.vertex(x, minX, maxY, maxZ).color(argbColor).uv(diffZ, diffY).endVertex();
        vertexConsumer.vertex(x, minX, minY, maxZ).color(argbColor).uv(diffZ, 0).endVertex();

        // Left exterior
        vertexConsumer.vertex(x, minX, minY, minZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, minX, minY, maxZ).color(argbColor).uv(0, diffZ).endVertex();
        vertexConsumer.vertex(x, minX, maxY, maxZ).color(argbColor).uv(diffY, diffZ).endVertex();
        vertexConsumer.vertex(x, minX, maxY, minZ).color(argbColor).uv(diffY, 0).endVertex();

        // Right interior
        vertexConsumer.vertex(x, maxX, minY, minZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, maxX, minY, maxZ).color(argbColor).uv(0, diffZ).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, maxZ).color(argbColor).uv(diffY, diffZ).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, minZ).color(argbColor).uv(diffY, 0).endVertex();

        // Right exterior
        vertexConsumer.vertex(x, maxX, minY, minZ).color(argbColor).uv(0, 0).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, minZ).color(argbColor).uv(0, diffY).endVertex();
        vertexConsumer.vertex(x, maxX, maxY, maxZ).color(argbColor).uv(diffZ, diffY).endVertex();
        vertexConsumer.vertex(x, maxX, minY, maxZ).color(argbColor).uv(diffZ, 0).endVertex();
    }

    private static final class Holder extends RenderStateShard {

        static ShaderInstance areaControlShader;

        private Holder(String name, Runnable setupCallback, Runnable cleanupCallback) {
            super(name, setupCallback, cleanupCallback);
        }

        //static final ShaderStateShard AREA_CONTROL_SHADER = new ShaderStateShard(() -> areaControlShader);

        // This is the vanilla world border texture; we are merely referring it, but using a custom texture state.
        static final EmptyTextureStateShard REPEATED_FORCE_FIELD = new TextureStateShard(new ResourceLocation("textures/misc/forcefield.png"), false, false);

        static final RenderType BORDER = RenderType.create("area_control_border",
                DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS, 256, false, false,
                RenderType.CompositeState.builder()
                        .setShaderState(POSITION_COLOR_TEX_SHADER) // Must be here
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setTextureState(REPEATED_FORCE_FIELD)
                        // 海螺 told me that vanilla avoids z-fighting during world border rendering
                        // by RenderSystem.enablePolygonOffset(), so here it is...
                        .setLayeringState(POLYGON_OFFSET_LAYERING)
                        .createCompositeState(false));

    }
}
